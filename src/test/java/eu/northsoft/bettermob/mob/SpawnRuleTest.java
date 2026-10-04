package eu.northsoft.bettermob.mob;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SpawnRuleTest {
    private static final long NOON = 6000;
    private static final long MIDNIGHT = 18000;

    @Test
    void emptyRuleMatchesEverywhereWithAFullChance() {
        SpawnRule rule = new SpawnRule(List.of(), List.of(), SpawnRule.Time.ANY, 1);
        assertTrue(rule.matches("world", "plains", NOON, 0.99));
    }

    @Test
    void worldAndBiomeAreMatchedIgnoringCaseAndNamespace() {
        SpawnRule rule = new SpawnRule(List.of("World_Nether"), List.of("minecraft:Desert"), SpawnRule.Time.ANY, 1);
        assertTrue(rule.matches("world_nether", "desert", NOON, 0));
        assertFalse(rule.matches("world", "desert", NOON, 0));
        assertFalse(rule.matches("world_nether", "plains", NOON, 0));
    }

    @Test
    void timeSeparatesDayFromNight() {
        SpawnRule night = new SpawnRule(List.of(), List.of(), SpawnRule.Time.NIGHT, 1);
        SpawnRule day = new SpawnRule(List.of(), List.of(), SpawnRule.Time.DAY, 1);
        assertTrue(night.matches("world", "plains", MIDNIGHT, 0));
        assertFalse(night.matches("world", "plains", NOON, 0));
        assertTrue(day.matches("world", "plains", NOON, 0));
        assertFalse(day.matches("world", "plains", MIDNIGHT, 0));
    }

    @Test
    void chanceIsRolledBelowTheThreshold() {
        SpawnRule rule = new SpawnRule(List.of(), List.of(), SpawnRule.Time.ANY, 0.25);
        assertTrue(rule.matches("world", "plains", NOON, 0.24));
        assertFalse(rule.matches("world", "plains", NOON, 0.25));
    }

    @Test
    void chanceIsClampedToTheValidRange() {
        assertTrue(new SpawnRule(List.of(), List.of(), SpawnRule.Time.ANY, 5).matches("world", "plains", NOON, 0.99));
        assertFalse(new SpawnRule(List.of(), List.of(), SpawnRule.Time.ANY, -1).matches("world", "plains", NOON, 0));
    }
}
