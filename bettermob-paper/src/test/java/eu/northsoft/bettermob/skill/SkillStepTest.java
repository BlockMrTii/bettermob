package eu.northsoft.bettermob.skill;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SkillStepTest {
    private static SkillStep.Mechanic mechanic(String line) {
        return assertInstanceOf(SkillStep.Mechanic.class, SkillStep.parse(line));
    }

    @Test
    void parsesMechanicWithParamsAndTargeter() {
        SkillStep.Mechanic step = mechanic("sound{s=entity.pig.hurt;p=1.5;v=1} @self");
        assertEquals("sound", step.name());
        assertEquals(Map.of("s", "entity.pig.hurt", "p", "1.5", "v", "1"), step.params());
        assertEquals("self", step.targeter());
        assertNull(step.inlineCondition());
        assertFalse(step.negated());
    }

    @Test
    void mechanicAndTargeterNamesAreLowercased() {
        SkillStep.Mechanic step = mechanic("CancelEvent @Self");
        assertEquals("cancelevent", step.name());
        assertEquals("self", step.targeter());
        assertTrue(step.params().isEmpty());
    }

    @Test
    void keepsNamespacedMechanicNames() {
        assertEquals("effect:particles", mechanic("effect:particles{p=flame;a=5} @self").name());
    }

    @Test
    void parsesTargeterParams() {
        SkillStep.Mechanic step = mechanic("damage{a=4} @EntitiesInRadius{r=5;t=player}");
        assertEquals("entitiesinradius", step.targeter());
        assertEquals(Map.of("r", "5", "t", "player"), step.targeterParams());
    }

    @Test
    void parsesInlineCondition() {
        SkillStep.Mechanic step = mechanic("message{m=hi} @self ?haspotion{type=SLOW}");
        assertEquals("haspotion{type=SLOW}", step.inlineCondition());
        assertFalse(step.negated());
    }

    @Test
    void parsesNegatedInlineCondition() {
        SkillStep.Mechanic step = mechanic("message{m=hi} @self ?!sneaking");
        assertEquals("sneaking", step.inlineCondition());
        assertTrue(step.negated());
    }

    @Test
    void semicolonsInsideNestedBracesDoNotSplitParams() {
        SkillStep.Mechanic step = mechanic("randomskill{s=[a{x=1;y=2};b];cd=2} @self");
        assertEquals("[a{x=1;y=2};b]", step.params().get("s"));
        assertEquals("2", step.params().get("cd"));
    }

    @Test
    void parsesDelay() {
        assertEquals(new SkillStep.Delay(20), SkillStep.parse("delay 20"));
        assertEquals(new SkillStep.Delay(5), SkillStep.parse("  Delay  5 "));
    }

    @Test
    void rejectsBadDelayAndEmptyLines() {
        assertNull(SkillStep.parse("delay soon"));
        assertNull(SkillStep.parse(""));
        assertNull(SkillStep.parse("   "));
    }

    @Test
    void parseParamsIgnoresPairsWithoutEquals() {
        assertEquals(Map.of("a", "1"), SkillStep.parseParams("a=1;oops"));
        assertTrue(SkillStep.parseParams(null).isEmpty());
        assertTrue(SkillStep.parseParams("  ").isEmpty());
    }

    @Test
    void inlineSplittingKeepsNestedSeparatorsTogether() {
        assertEquals(
                List.of("sound{s=a;p=1} @self", "delay 5", "message{m=hi - there}"),
                SkillEngine.splitInline("[sound{s=a;p=1} @self - delay 5 - message{m=hi - there}]"));
    }

    @Test
    void inlineSplittingDropsLeadingSeparatorAndKeepsHyphenatedWords() {
        assertEquals(List.of("a{}", "b-c{}"), SkillEngine.splitInline("- a{} - b-c{}"));
    }
}
