package eu.northsoft.bettermob.stats;

import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SkillStatsTest {
    private static final long MILLI = 1_000_000L;

    @Test
    void recordsCallsAverageMaxAndTotal() {
        SkillStats stats = new SkillStats();
        stats.record("fire", 2 * MILLI);
        stats.record("fire", 6 * MILLI);
        SkillStats.Row row = stats.top(5).get(0);
        assertEquals("fire", row.skill());
        assertEquals(2, row.calls());
        assertEquals(4.0, row.averageMillis(), 1e-9);
        assertEquals(6.0, row.maxMillis(), 1e-9);
        assertEquals(8.0, row.totalMillis(), 1e-9);
    }

    @Test
    void topIsSortedByTotalTimeAndLimited() {
        SkillStats stats = new SkillStats();
        stats.record("small", MILLI);
        stats.record("big", 9 * MILLI);
        stats.record("medium", 4 * MILLI);
        List<SkillStats.Row> top = stats.top(2);
        assertEquals(List.of("big", "medium"), top.stream().map(SkillStats.Row::skill).toList());
    }

    @Test
    void resetClearsTimingsAndPendingSteps() {
        SkillStats stats = new SkillStats();
        stats.enabled(true);
        stats.record("fire", MILLI);
        stats.trackPending(() -> { });
        stats.reset();
        assertTrue(stats.top(5).isEmpty());
        assertEquals(0, stats.pending());
    }

    @Test
    void pendingStepsAreCountedUntilTheyRun() {
        SkillStats stats = new SkillStats();
        stats.enabled(true);
        AtomicInteger ran = new AtomicInteger();
        Runnable first = stats.trackPending(ran::incrementAndGet);
        Runnable second = stats.trackPending(ran::incrementAndGet);
        assertEquals(2, stats.pending());
        first.run();
        assertEquals(1, stats.pending());
        second.run();
        assertEquals(0, stats.pending());
        assertEquals(2, ran.get());
    }

    @Test
    void whileOffTrackingReturnsTheSameTaskAndCountsNothing() {
        SkillStats stats = new SkillStats();
        Runnable task = () -> { };
        assertSame(task, stats.trackPending(task));
        assertEquals(0, stats.pending());
    }

    @Test
    void disablingDoesNotLeaveTheCounterNegative() {
        SkillStats stats = new SkillStats();
        stats.enabled(true);
        Runnable tracked = stats.trackPending(() -> { });
        stats.enabled(false);
        stats.reset();
        tracked.run();
        assertEquals(0, stats.pending());
    }

    @Test
    void readsTheSettingsFromTheConfig() {
        SkillStats stats = new SkillStats();
        YamlConfiguration config = new YamlConfiguration();
        assertFalse(stats.enabled());
        config.set("Stats", "on");
        config.set("StatsWarnMillis", 12.5);
        stats.reload(config);
        assertTrue(stats.enabled());
        assertEquals(12.5, stats.warnMillis(), 1e-9);

        stats.reload(new YamlConfiguration());
        assertFalse(stats.enabled());
        assertEquals(50, stats.warnMillis(), 1e-9);
    }
}
