package eu.northsoft.bettermob.skill;

import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SubstitutionsTest {

    @Test
    void replacesSkillAndCasterVariables() {
        Map<String, String> skillScope = Map.of("power", "100");
        Map<String, String> casterScope = Map.of("level", "5");

        String result = Substitutions.replaceVariables("Power: <skill.var.power>, Level: <caster.var.level>", skillScope, casterScope);
        assertEquals("Power: 100, Level: 5", result);
    }

    @Test
    void replacesLocationValues() {
        String template = "x=<caster.l.x>, y=<caster.l.y>, z=<caster.l.z>";
        String result = Substitutions.withLocationValues(template, 10.5, 64.0, -20.25, 90f, 0f);
        assertEquals("x=10.5, y=64, z=-20.25", result);
    }

    @Test
    void replacesCasterNameAndDamage() {
        String template = "Hit by <caster.name> for <caster.damage> dmg";
        String result = Substitutions.withCasterValues(template, "12", "BossMob", false);
        assertEquals("Hit by BossMob for 12 dmg", result);
    }

    @Test
    void detectsInlineSkill() {
        assertTrue(Substitutions.isInlineSkill("[ - damage{a=5} ]"));
    }
}
