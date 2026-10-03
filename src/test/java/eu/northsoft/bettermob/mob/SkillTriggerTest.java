package eu.northsoft.bettermob.mob;

import eu.northsoft.bettermob.mob.MobDefinition.SkillTrigger;
import eu.northsoft.bettermob.mob.MobDefinition.SkillTrigger.Trigger;
import eu.northsoft.bettermob.skill.SkillStep;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SkillTriggerTest {
    @Test
    void parsesPlainTrigger() {
        SkillTrigger trigger = SkillTrigger.parse("skill{s=my_skill_id} ~onInteract");
        assertEquals(Trigger.INTERACT, trigger.trigger());
        assertEquals(20, trigger.timerTicks());
        assertEquals("skill", assertInstanceOf(SkillStep.Mechanic.class, trigger.step()).name());
    }

    @Test
    void parsesTimerInterval() {
        SkillTrigger trigger = SkillTrigger.parse("sound{s=entity.skeleton.ambient;p=1.0;v=1} @self ~onTimer:200");
        assertEquals(Trigger.TIMER, trigger.trigger());
        assertEquals(200, trigger.timerTicks());
        assertEquals("self", ((SkillStep.Mechanic) trigger.step()).targeter());
    }

    @Test
    void parsesConditionAfterTheTrigger() {
        SkillTrigger trigger = SkillTrigger.parse("message{m=hi} ~onDamaged ?!sneaking");
        SkillStep.Mechanic step = (SkillStep.Mechanic) trigger.step();
        assertEquals(Trigger.DAMAGED, trigger.trigger());
        assertEquals("sneaking", step.inlineCondition());
        assertTrue(step.negated());
    }

    @Test
    void triggerNamesAreCaseInsensitiveAndHaveAliases() {
        assertEquals(Trigger.SPAWN, SkillTrigger.parse("remove @self ~onSPAWN").trigger());
        assertEquals(Trigger.INTERACT, SkillTrigger.parse("remove @self ~onRightClick").trigger());
        assertEquals(Trigger.DAMAGED, SkillTrigger.parse("remove @self ~onHurt").trigger());
    }

    @Test
    void everyTriggerParses() {
        for (String name : new String[]{"spawn", "load", "interact", "damaged", "attack", "death", "timer", "use", "shoot"}) {
            assertEquals(Trigger.valueOf(name.toUpperCase()), SkillTrigger.parse("remove @self ~on" + name).trigger(), name);
        }
    }

    @Test
    void rejectsUnknownTriggersAndLinesWithoutOne() {
        assertNull(SkillTrigger.parse("remove @self ~onNothing"));
        assertNull(SkillTrigger.parse("remove @self"));
        assertNull(SkillTrigger.parse("~onSpawn"));
    }
}
