package eu.northsoft.bettermob.drop;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DropEntryTest {
    private static void check(DropEntry entry, String name, int min, int max, double chance) {
        assertEquals(name, entry.name());
        assertEquals(min, entry.min());
        assertEquals(max, entry.max());
        assertEquals(chance, entry.chance(), 1e-9);
    }

    @Test
    void parsesNameAmountRangeAndPercentChance() {
        check(DropEntry.parse("LEATHER 4-5 80%"), "LEATHER", 4, 5, 0.8);
        check(DropEntry.parse("nm_bison_fur 2-3 40%"), "nm_bison_fur", 2, 3, 0.4);
    }

    @Test
    void parsesExperienceLine() {
        DropEntry entry = DropEntry.parse("EXP 5-11 100%");
        check(entry, "EXP", 5, 11, 1.0);
        assertTrue(entry.isExp());
        assertTrue(DropEntry.parse("xp 1").isExp());
        assertFalse(DropEntry.parse("DIAMOND").isExp());
    }

    @Test
    void amountAndChanceAreOptional() {
        check(DropEntry.parse("DIAMOND"), "DIAMOND", 1, 1, 1.0);
        check(DropEntry.parse("DIAMOND 3"), "DIAMOND", 3, 3, 1.0);
        check(DropEntry.parse("DIAMOND 5%"), "DIAMOND", 1, 1, 0.05);
        check(DropEntry.parse("DIAMOND 0.25"), "DIAMOND", 1, 1, 0.25);
    }

    @Test
    void acceptsDecimalChanceAfterAmount() {
        check(DropEntry.parse("DIAMOND 1 0.05"), "DIAMOND", 1, 1, 0.05);
        check(DropEntry.parse("DIAMOND 1 5%"), "DIAMOND", 1, 1, 0.05);
    }

    @Test
    void supportsToAsRangeSeparatorAndSwapsReversedRanges() {
        check(DropEntry.parse("DIAMOND 2to4 10%"), "DIAMOND", 2, 4, 0.1);
        check(DropEntry.parse("DIAMOND 5-2 10%"), "DIAMOND", 2, 5, 0.1);
    }

    @Test
    void clampsChance() {
        check(DropEntry.parse("DIAMOND 1 150%"), "DIAMOND", 1, 1, 1.0);
        check(DropEntry.parse("DIAMOND 1 -5%"), "DIAMOND", 1, 1, 0.0);
    }

    @Test
    void rejectsGarbage() {
        assertNull(DropEntry.parse("DIAMOND lots"));
        assertNull(DropEntry.parse("DIAMOND 1 often"));
        assertNull(DropEntry.parse(""));
    }
}
