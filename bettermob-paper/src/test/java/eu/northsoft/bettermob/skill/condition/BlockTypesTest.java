package eu.northsoft.bettermob.skill.condition;

import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;

class BlockTypesTest {
    @Test
    void parseReadsTheListAfterTheKeyCaseInsensitively() {
        assertEquals(Set.of("STONE", "OAK_LOG"), BlockTypes.parse("t=stone, oak_log"));
        assertEquals(Set.of("DIRT"), BlockTypes.parse("Dirt"));
    }
}
