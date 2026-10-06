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

    @Test
    void parsesTheHealthTriggerWithItsThreshold() {
        SkillTrigger percent = SkillTrigger.parse("skill{s=enrage} @self ~onHealth<50%");
        assertEquals(Trigger.HEALTH, percent.trigger());
        assertEquals(new HealthSpec("<", 50, true), percent.health());
        SkillTrigger absolute = SkillTrigger.parse("message{m=low} ~onHealth<=10.5 ?chance{chance=0.5}");
        assertEquals(new HealthSpec("<=", 10.5, false), absolute.health());
        assertEquals("chance{chance=0.5}", ((SkillStep.Mechanic) absolute.step()).inlineCondition());
        assertEquals(new HealthSpec(">=", 80, true), SkillTrigger.parse("skill{s=calm} @self ~onHealth>=80%").health());
    }

    @Test
    void theHealthTriggerNeedsAThresholdAndOthersMustNotHaveOne() {
        assertNull(SkillTrigger.parse("skill{s=x} @self ~onHealth"));
        assertNull(SkillTrigger.parse("skill{s=x} @self ~onDamaged<5"));
    }

    @Test
    void parsesTheTargetAndCombatTriggers() {
        assertEquals(Trigger.TARGET, SkillTrigger.parse("skill{s=x} @self ~onTarget").trigger());
        assertEquals(Trigger.LOSETARGET, SkillTrigger.parse("skill{s=x} @self ~onLoseTarget").trigger());
        assertEquals(Trigger.ENTERCOMBAT, SkillTrigger.parse("skill{s=x} @self ~onEnterCombat").trigger());
        assertEquals(Trigger.EXITCOMBAT, SkillTrigger.parse("skill{s=x} @self ~onExitCombat").trigger());
        assertEquals(Trigger.KILL, SkillTrigger.parse("skill{s=x} @self ~onKill").trigger());
    }

    @Test
    void healthSpecsCompareAbsoluteValuesAndPercentages() {
        HealthSpec half = new HealthSpec("<", 50, true);
        assertTrue(half.matches(9, 20));
        assertTrue(!half.matches(10, 20));
        HealthSpec low = new HealthSpec("<=", 4, false);
        assertTrue(low.matches(4, 20));
        assertTrue(!low.matches(4.5, 20));
        assertTrue(new HealthSpec(">=", 80, true).matches(16, 20));
        assertTrue(new HealthSpec(">", 0, false).matches(1, 20));
    }
}
