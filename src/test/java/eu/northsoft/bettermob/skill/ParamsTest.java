package eu.northsoft.bettermob.skill;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ParamsTest {
    @Test
    void stripSkillSyntaxRemovesCharactersThatSplitInlineSkills() {
        assertEquals("x  commandcop Evil", Params.stripSkillSyntax("x} - command{c=op Evil"));
        assertEquals("Zombie 1", Params.stripSkillSyntax("Zombie [1];"));
    }
}
