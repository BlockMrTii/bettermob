package eu.northsoft.bettermob.skill;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ParamsTest {
    @Test
    void stripSkillSyntaxRemovesCharactersThatSplitInlineSkills() {
        assertEquals("x  commandcop Evil", Params.stripSkillSyntax("x} - command{c=op Evil"));
        assertEquals("Zombie 1", Params.stripSkillSyntax("Zombie [1];"));
    }

    @org.junit.jupiter.api.Test
    void numbersMayBeCalculations() {
        org.junit.jupiter.api.Assertions.assertEquals(69.0, Params.parseNumber("64+5", 0));
        org.junit.jupiter.api.Assertions.assertEquals(1.5, Params.parseNumber("\"1.5\"", 0));
        org.junit.jupiter.api.Assertions.assertEquals(-2.0, Params.parseNumber("nope", -2));
        org.junit.jupiter.api.Assertions.assertEquals(-2.0, Params.parseNumber(null, -2));
    }
}
