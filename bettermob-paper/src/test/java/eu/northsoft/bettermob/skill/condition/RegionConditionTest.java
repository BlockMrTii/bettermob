package eu.northsoft.bettermob.skill.condition;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;

class RegionConditionTest {
    @Test
    void theRegionListIsSplitLowerCasedAndTrimmed() {
        assertArrayEquals(new String[]{"spawn", "arena_1"}, RegionCondition.names("Spawn, Arena_1"));
        assertArrayEquals(new String[]{"spawn"}, RegionCondition.names("SPAWN"));
    }
}
