package eu.northsoft.bettermob.stats;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SkillStatsTest {
    @Test
    void mechanicsAreAggregatedAndRankedByTotalTime() {
        SkillStats stats = new SkillStats();
        stats.recordMechanic("damage", 2_000_000);
        stats.recordMechanic("damage", 4_000_000);
        stats.recordMechanic("sound", 1_000_000);
        stats.recordMechanic("totem", 9_000_000);
        List<SkillStats.Row> top = stats.topMechanics(2);
        assertEquals(List.of("totem", "damage"), top.stream().map(SkillStats.Row::skill).toList());
        assertEquals(2, top.get(1).calls());
        assertEquals(3.0, top.get(1).averageMillis(), 1.0e-9);
        assertEquals(4.0, top.get(1).maxMillis(), 1.0e-9);
        assertEquals(6.0, top.get(1).totalMillis(), 1.0e-9);
    }

    @Test
    void skillsAndMechanicsAreSeparateAndResetClearsBoth() {
        SkillStats stats = new SkillStats();
        stats.record("boss", 5_000_000);
        stats.recordMechanic("damage", 1_000_000);
        assertEquals(1, stats.top(10).size());
        assertEquals(1, stats.topMechanics(10).size());
        stats.reset();
        assertTrue(stats.top(10).isEmpty());
        assertTrue(stats.topMechanics(10).isEmpty());
    }
}
