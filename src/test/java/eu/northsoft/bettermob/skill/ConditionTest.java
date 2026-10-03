package eu.northsoft.bettermob.skill;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class ConditionTest {
    @Test
    void parsesBareName() {
        assertEquals(new Condition("offgcd", null, null, null, null), Condition.parse("offgcd"));
    }

    @Test
    void parsesParamsExpectedValueAndAction() {
        assertEquals(new Condition("onground", "", true, "cancel", null), Condition.parse("onground{} true cancel"));
    }

    @Test
    void actionValueKeepsTheRestOfTheLine() {
        Condition condition = Condition.parse("Health{h=<50%} false message you are too   healthy");
        assertEquals("health", condition.name());
        assertEquals("h=<50%", condition.params());
        assertEquals(false, condition.expected());
        assertEquals("message", condition.action());
        assertEquals("you are too healthy", condition.actionValue());
    }

    @Test
    void actionWithoutExpectedValue() {
        Condition condition = Condition.parse("sneaking cancelskill");
        assertNull(condition.expected());
        assertEquals("cancelskill", condition.action());
    }

    @Test
    void nestedBracesStayInsideParams() {
        assertEquals("a=[x{y=1}]", Condition.parse("any{a=[x{y=1}]} true").params());
    }

    @Test
    void rejectsLinesWithoutAName() {
        assertNull(Condition.parse("{a=1}"));
        assertNull(Condition.parse(""));
    }
}
