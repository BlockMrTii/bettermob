package eu.northsoft.bettermob.skill;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class LocationPlaceholdersTest {
    @Test
    void casterLocationPlaceholdersBecomeNumbers() {
        assertEquals("yaw=90 pitch=-12.5", SkillEngine.withLocationValues("yaw=<caster.l.yaw> pitch=<caster.l.pitch>", 1, 2, 3, 90f, -12.5f));
        assertEquals("1 64.5 -3", SkillEngine.withLocationValues("<caster.l.x> <caster.l.y> <caster.l.z>", 1, 64.5, -3, 0f, 0f));
        assertEquals("<caster.name>", SkillEngine.withLocationValues("<caster.name>", 1, 2, 3, 0f, 0f));
    }
}
