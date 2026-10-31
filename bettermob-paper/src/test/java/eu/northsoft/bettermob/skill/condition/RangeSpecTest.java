package eu.northsoft.bettermob.skill.condition;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RangeSpecTest {
    @Test
    void comparisonsAndRanges() {
        assertTrue(RangeSpec.matches("<50", 20));
        assertFalse(RangeSpec.matches("<50", 50));
        assertTrue(RangeSpec.matches("<=50", 50));
        assertTrue(RangeSpec.matches(">10", 11));
        assertTrue(RangeSpec.matches(">=10", 10));
        assertTrue(RangeSpec.matches("0-6", 6));
        assertFalse(RangeSpec.matches("0-6", 6.5));
    }

    @Test
    void singleValueAllowsHalfAPointOfSlack() {
        assertTrue(RangeSpec.matches("20", 20.4));
        assertFalse(RangeSpec.matches("20", 21));
    }

    @Test
    void garbageNeverMatches() {
        assertFalse(RangeSpec.matches("abc", 1));
        assertFalse(RangeSpec.matches("", 1));
        assertFalse(RangeSpec.matches(null, 1));
    }

    @Test
    void nightStartsAt13000AndEndsBeforeSunrise() {
        assertFalse(TimeCondition.isNight(12999));
        assertTrue(TimeCondition.isNight(13000));
        assertTrue(TimeCondition.isNight(22999));
        assertFalse(TimeCondition.isNight(23000));
    }
}
