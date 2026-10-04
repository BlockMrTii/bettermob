package eu.northsoft.bettermob.skill;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class SkillEngineTest {
    @Test
    void commandAndMessageKeepTheCasterNamePlaceholderForTheirOwnEscaping() {
        assertEquals("lp user <caster.name> 5", SkillEngine.withCasterValues("lp user <caster.name> <caster.damage>", "5", "Bob parent add admin", true));
    }

    @Test
    void inlineSkillsKeepTheCasterNamePlaceholderForTheNestedMechanic() {
        assertEquals("command{c=kill <caster.name>}", SkillEngine.withCasterValues("command{c=kill <caster.name>}", "5", "@a", false));
    }

    @Test
    void otherMechanicsGetTheCasterName() {
        assertEquals("Hello Bob", SkillEngine.withCasterValues("Hello <caster.name>", "5", "Bob", false));
    }
}
