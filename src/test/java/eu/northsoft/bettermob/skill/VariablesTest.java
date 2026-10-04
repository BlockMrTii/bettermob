package eu.northsoft.bettermob.skill;

import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class VariablesTest {
    @Test
    void namesAndScopes() {
        assertTrue(Variables.isCasterScope("caster.hits"));
        assertFalse(Variables.isCasterScope("hits"));
        assertEquals("hits", Variables.nameOf("Caster.Hits"));
        assertEquals("phase", Variables.nameOf("phase"));
        assertNull(Variables.nameOf("bad name"));
        assertNull(Variables.nameOf("a{b}"));
        assertNull(Variables.nameOf(""));
    }

    @Test
    void valuesAreNormalisedByType() {
        assertEquals("3", Variables.normalise("3.7", Variables.Type.INTEGER));
        assertEquals("2.5", Variables.normalise("2.5", Variables.Type.FLOAT));
        assertEquals("2", Variables.normalise("2.0", Variables.Type.FLOAT));
        assertEquals(" hello ", Variables.normalise(" hello ", Variables.Type.STRING));
        assertNull(Variables.normalise("abc", Variables.Type.INTEGER));
        assertEquals(Variables.Type.INTEGER, Variables.typeOf("int"));
        assertEquals(Variables.Type.STRING, Variables.typeOf(null));
    }

    @Test
    void addTreatsAMissingValueAsZero() {
        assertEquals("1", Variables.add(null, "1"));
        assertEquals("3", Variables.add("2", "1"));
        assertEquals("0.5", Variables.add("1", "-0.5"));
        assertNull(Variables.add("x", "1"));
        assertNull(Variables.add("1", "y"));
    }

    @Test
    void aScopeHoldsAtMostSixtyFourVariables() {
        Map<String, String> scope = new HashMap<>();
        for (int i = 0; i < Variables.MAX_PER_SCOPE; i++) assertTrue(Variables.put(scope, "v" + i, "1"));
        assertFalse(Variables.put(scope, "extra", "1"));
        assertTrue(Variables.put(scope, "v0", "2"));
    }

    @Test
    void matchingUsesNumbersWhereItCanAndTextOtherwise() {
        assertTrue(Variables.matches(">3", "5"));
        assertFalse(Variables.matches(">3", "2"));
        assertTrue(Variables.matches("0", null));
        assertTrue(Variables.matches("<=1", null));
        assertTrue(Variables.matches("boss", "Boss"));
        assertFalse(Variables.matches("boss", null));
        assertFalse(Variables.matches("boss", "minion"));
    }

    @Test
    void sanitizeRemovesSkillSyntaxButKeepsNegativeNumbers() {
        assertEquals("-5", Variables.sanitize("-5"));
        assertEquals("Rex  commandc", Variables.sanitize("Rex} - command{c"));
        assertFalse(Variables.sanitize("a%b;c=d").matches(".*[%;=].*"));
    }

    @Test
    void placeholdersReadTheRightScopeAndDefaultToZero() {
        Map<String, String> skillScope = Map.of("hits", "3");
        Map<String, String> casterScope = Map.of("phase", "2");
        assertEquals("a=3 b=2 c=0 d=0", SkillEngine.replaceVariables("a=<skill.hits> b=<var.phase> c=<skill.none> d=<var.hits>", skillScope, casterScope));
        assertEquals("0", SkillEngine.replaceVariables("<var.phase>", skillScope, null));
        assertEquals("<caster.name>", SkillEngine.replaceVariables("<caster.name>", skillScope, casterScope));
    }
}
