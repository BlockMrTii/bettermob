package eu.northsoft.bettermob.stats;

import org.bukkit.configuration.ConfigurationSection;

import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.LongAdder;

public final class SkillStats {
    public record Row(String skill, long calls, double averageMillis, double maxMillis, double totalMillis) {}

    private static final class Entry {
        final LongAdder calls = new LongAdder();
        final LongAdder totalNanos = new LongAdder();
        final AtomicLong maxNanos = new AtomicLong();
    }

    private static final double DEFAULT_WARN_MILLIS = 50;

    private final Map<String, Entry> entries = new ConcurrentHashMap<>();
    private final AtomicInteger pending = new AtomicInteger();
    private volatile boolean enabled;
    private volatile double warnMillis = DEFAULT_WARN_MILLIS;

    public void reload(ConfigurationSection config) {
        String configured = String.valueOf(config.get("Stats", "off")).toLowerCase(Locale.ROOT);
        enabled = configured.equals("on") || configured.equals("true");
        warnMillis = config.getDouble("StatsWarnMillis", DEFAULT_WARN_MILLIS);
    }

    public boolean enabled() {
        return enabled;
    }

    public void enabled(boolean enabled) {
        this.enabled = enabled;
    }

    public double warnMillis() {
        return warnMillis;
    }

    public void record(String skill, long nanos) {
        Entry entry = entries.computeIfAbsent(skill, key -> new Entry());
        entry.calls.increment();
        entry.totalNanos.add(nanos);
        entry.maxNanos.accumulateAndGet(nanos, Math::max);
    }

    public List<Row> top(int limit) {
        return entries.entrySet().stream()
                .map(entry -> row(entry.getKey(), entry.getValue()))
                .sorted(Comparator.comparingDouble(Row::totalMillis).reversed())
                .limit(limit)
                .toList();
    }

    public Runnable trackPending(Runnable task) {
        if (!enabled) return task;
        pending.incrementAndGet();
        return () -> {
            pending.decrementAndGet();
            task.run();
        };
    }

    public int pending() {
        return Math.max(0, pending.get());
    }

    public void reset() {
        entries.clear();
        pending.set(0);
    }

    private static Row row(String skill, Entry entry) {
        long calls = entry.calls.sum();
        double total = entry.totalNanos.sum() / 1e6;
        return new Row(skill, calls, calls == 0 ? 0 : total / calls, entry.maxNanos.get() / 1e6, total);
    }
}
