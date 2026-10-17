package eu.northsoft.bettermob.skill;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class ConditionTest {
    @Test
    void compositeConditionsCombineOrAndAndWithNesting() {
        Condition condition = Condition.parse("( a{x=1} || b false && ( c || d ) ) false");
        assertEquals(Condition.COMPOSITE, condition.name());
        assertEquals(false, condition.expected());
        java.util.Set<String> holding = java.util.Set.of("b", "d");
        java.util.function.Predicate<Condition> met = c -> holding.contains(c.name()) == (c.expected() == null || c.expected());
        org.junit.jupiter.api.Assertions.assertFalse(Condition.holds(condition.params(), met));
        java.util.Set<String> withA = java.util.Set.of("a");
        org.junit.jupiter.api.Assertions.assertTrue(Condition.holds(condition.params(),
                c -> c.composite() ? Condition.holds(c.params(), x -> withA.contains(x.name())) : withA.contains(c.name()) == (c.expected() == null || c.expected())));
        Condition both = Condition.parse("(a) && (b)");
        assertEquals(Condition.COMPOSITE, both.name());
        assertNull(both.action());
        java.util.function.Predicate<Condition> onlyA = c -> c.composite() ? Condition.holds(c.params(), x -> x.name().equals("a")) : c.name().equals("a");
        org.junit.jupiter.api.Assertions.assertFalse(Condition.holds(both.params(), onlyA));
        org.junit.jupiter.api.Assertions.assertTrue(Condition.parse("( a || blockType{type=OAK_LOG} )").mentions("blocktype"));
        org.junit.jupiter.api.Assertions.assertFalse(Condition.parse("( varequals{var=blocktype;val=1} )").mentions("blocktype"));
        assertNull(Condition.parse("(a) &&"));
        assertNull(Condition.parse("( a || )"));
        assertNull(Condition.parse("( a || b"));
    }

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
