package eu.northsoft.bettermob.skill.condition;

import eu.northsoft.bettermob.skill.Condition;
import eu.northsoft.bettermob.skill.SkillContext;
import eu.northsoft.bettermob.skill.Target;
import org.bukkit.Location;
import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ConditionRegistryTest {
    private final ConditionRegistry registry = new ConditionRegistry(null);

    @Test
    void customConditionsAreRegisteredOnceAndNeverTakeABuiltinName() {
        assertTrue(registry.registerCustom(null, "pettaming", context -> true));
        assertFalse(registry.registerCustom(null, "PetTaming", context -> true));
        assertFalse(registry.registerCustom(null, "health", context -> true));
        assertTrue(registry.has("pettaming"));
        registry.unregisterCustom("pettaming");
        assertFalse(registry.has("pettaming"));
    }

    @Test
    void aCustomConditionReceivesTheTargetOverride() {
        AtomicReference<Double> x = new AtomicReference<>();
        registry.registerCustom(null, "atx", context -> {
            x.set(context.location() == null ? null : context.location().getX());
            return context.location() != null;
        });
        Target target = Target.ofLocation(new Location(null, 7, 2, 3));
        assertTrue(registry.evaluate(Condition.parse("atx{}"), new SkillContext(null, null, null), target));
        assertEquals(7.0, x.get());
        assertFalse(registry.evaluate(Condition.parse("atx{}"), new SkillContext(null, null, null), null));
    }

    @Test
    void aCustomConditionReceivesItsParameters() {
        AtomicReference<String> level = new AtomicReference<>();
        registry.registerCustom(null, "petlevel", context -> {
            level.set(context.params().get("level"));
            return "3".equals(context.params().get("level"));
        });
        assertTrue(registry.evaluate(Condition.parse("petlevel{level=3}"), new SkillContext(null, null, null), null));
        assertEquals("3", level.get());
        assertFalse(registry.evaluate(Condition.parse("petlevel{level=4}"), new SkillContext(null, null, null), null));
    }
}
