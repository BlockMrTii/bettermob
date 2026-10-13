package eu.northsoft.bettermob.mob;

import org.bukkit.plugin.Plugin;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class ExtraTriggers {
    private record Entry(Plugin owner) {}

    private record Pending(String line, Runnable warn) {}

    private static final Pattern TAIL = Pattern.compile("~on(\\w+?)(?::\\S*)?(?:\\s+\\?\\S.*)?\\s*$", Pattern.CASE_INSENSITIVE);
    private static final Map<String, Entry> TRIGGERS = new ConcurrentHashMap<>();
    private static final List<Pending> PENDING = new ArrayList<>();
    private static boolean ready;

    private ExtraTriggers() {
    }

    private static String key(String name) {
        String lower = name.trim().toLowerCase(Locale.ROOT);
        return lower.startsWith("on") && lower.length() > 2 ? lower.substring(2) : lower;
    }

    public static boolean register(Plugin owner, String name) {
        if (name == null || name.isBlank()) return false;
        String key = key(name);
        if (key.isEmpty() || MobDefinition.SkillTrigger.Trigger.parse(key) != null) return false;
        return TRIGGERS.putIfAbsent(key, new Entry(owner)) == null;
    }

    public static void unregister(String name) {
        if (name != null) TRIGGERS.remove(key(name));
    }

    public static void unregister(Plugin owner) {
        TRIGGERS.values().removeIf(entry -> java.util.Objects.equals(entry.owner(), owner));
    }

    public static boolean accepts(String line) {
        if (line == null || TRIGGERS.isEmpty()) return false;
        Matcher matcher = TAIL.matcher(line.trim());
        return matcher.find() && TRIGGERS.containsKey(matcher.group(1).toLowerCase(Locale.ROOT));
    }

    public static synchronized void warnUnparsed(String line, Runnable warn) {
        if (accepts(line)) return;
        if (ready) warn.run();
        else PENDING.add(new Pending(line, warn));
    }

    public static synchronized void ready() {
        ready = true;
        for (Pending pending : PENDING) {
            if (!accepts(pending.line())) pending.warn().run();
        }
        PENDING.clear();
    }
}
