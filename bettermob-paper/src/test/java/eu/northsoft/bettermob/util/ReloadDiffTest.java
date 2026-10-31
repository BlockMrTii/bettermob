package eu.northsoft.bettermob.util;

import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ReloadDiffTest {
    @Test
    void newRemovedAndChangedIdsAreSortedAndSeparated() {
        ReloadDiff diff = ReloadDiff.of(Map.of("a", "1", "b", "2", "c", "3"), Map.of("b", "2", "c", "9", "z", "1", "d", "5"));
        assertEquals(List.of("d", "z"), diff.added());
        assertEquals(List.of("a"), diff.removed());
        assertEquals(List.of("c"), diff.changed());
        assertTrue(ReloadDiff.of(Map.of("a", "1"), Map.of("a", "1")).isEmpty());
    }

    @Test
    void theFingerprintFollowsTheContentOfASection() throws Exception {
        YamlConfiguration first = new YamlConfiguration();
        first.loadFromString("Health: 20\nSkills:\n  - a\n");
        YamlConfiguration same = new YamlConfiguration();
        same.loadFromString("Health: 20\nSkills:\n  - a\n");
        YamlConfiguration other = new YamlConfiguration();
        other.loadFromString("Health: 21\nSkills:\n  - a\n");
        assertEquals(ReloadDiff.fingerprint(first.getValues(true)), ReloadDiff.fingerprint(same.getValues(true)));
        assertNotEquals(ReloadDiff.fingerprint(first.getValues(true)), ReloadDiff.fingerprint(other.getValues(true)));
    }

    @Test
    void valuesWithTheSameJavaHashCodeStillDiffer() throws Exception {
        assertEquals("Aa".hashCode(), "BB".hashCode());
        YamlConfiguration first = new YamlConfiguration();
        first.loadFromString("Display: Aa\n");
        YamlConfiguration second = new YamlConfiguration();
        second.loadFromString("Display: BB\n");
        assertNotEquals(ReloadDiff.fingerprint(first.getValues(true)), ReloadDiff.fingerprint(second.getValues(true)));
    }
}
