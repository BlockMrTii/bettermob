package eu.northsoft.bettermob.skill.condition;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class KillConditionTest {
    @Test
    void aPlainNumberMeansAtLeastThatManyKills() {
        assertEquals(">=10", KillCondition.atLeast("10"));
        assertTrue(RangeSpec.matches(KillCondition.atLeast("10"), 10));
        assertTrue(RangeSpec.matches(KillCondition.atLeast("10"), 25));
        assertFalse(RangeSpec.matches(KillCondition.atLeast("10"), 9));
    }

    @Test
    void comparatorsAndRangesAreKept() {
        assertEquals("<5", KillCondition.atLeast("<5"));
        assertEquals("3-6", KillCondition.atLeast("3-6"));
        assertTrue(RangeSpec.matches(KillCondition.atLeast("3-6"), 4));
        assertFalse(RangeSpec.matches(KillCondition.atLeast("3-6"), 7));
        assertFalse(RangeSpec.matches(KillCondition.atLeast(""), 1));
    }
}
