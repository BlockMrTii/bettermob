package eu.northsoft.bettermob.skill.target;

import eu.northsoft.bettermob.skill.SkillContext;
import eu.northsoft.bettermob.skill.Target;
import org.bukkit.Bukkit;
import org.bukkit.entity.LivingEntity;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;

class TargeterRegistryTest {
    private final TargeterRegistry registry = new TargeterRegistry(null);
    private final LivingEntity caster = mock(LivingEntity.class);
    private final LivingEntity trigger = mock(LivingEntity.class);

    @Test
    void customTargetersAreRegisteredOnceAndNeverTakeABuiltinName() {
        assertTrue(registry.registerCustom(null, "petowner", context -> List.of()));
        assertFalse(registry.registerCustom(null, "PetOwner", context -> List.of()));
        assertFalse(registry.registerCustom(null, "self", context -> List.of()));
        assertTrue(registry.has("petowner"));
        registry.unregisterCustom("petowner");
        assertFalse(registry.has("petowner"));
    }

    @Test
    void livingInConeIsRegisteredAndChecksTheHorizontalAngle() {
        assertTrue(registry.has("LivingInCone"));

        assertTrue(EntitiesInRadiusTargeter.inCone(0, 45, 0, 5));
        assertTrue(EntitiesInRadiusTargeter.inCone(0, 90, 5, 0.01));
        assertFalse(EntitiesInRadiusTargeter.inCone(0, 45, 5, 1));
        assertFalse(EntitiesInRadiusTargeter.inCone(0, 90, 0, -5));

        assertTrue(EntitiesInRadiusTargeter.inCone(90, 10, -5, 0));
        assertTrue(EntitiesInRadiusTargeter.inCone(-170, 20, 0.5, -5));
    }

    private SkillContext inAnotherRegion(SkillContext context) {
        try (MockedStatic<Bukkit> bukkit = mockStatic(Bukkit.class)) {
            bukkit.when(() -> Bukkit.isOwnedByCurrentRegion(trigger)).thenReturn(false);
            return context.ownedHere();
        }
    }

    private static LivingEntity only(List<Target> targets) {
        assertEquals(1, targets.size());
        return (LivingEntity) targets.get(0).entity();
    }

    @Test
    void triggerAndTargetKeepATriggerOfAnotherRegion() {
        SkillContext context = inAnotherRegion(new SkillContext(caster, trigger, null));
        assertSame(trigger, only(registry.resolveAll("trigger", Map.of(), context)));
        assertSame(trigger, only(registry.resolveAll("target", Map.of(), context)));
    }

    @Test
    void theInheritedTargetKeepsATriggerOfAnotherRegion() {
        SkillContext context = inAnotherRegion(SkillContext.of(caster).withTrigger(trigger));
        assertSame(trigger, only(registry.resolveAll("", Map.of(), context)));
    }

    @Test
    void theInheritedTargetStaysTheCasterWhenTheTriggerIsNotTheTarget() {
        SkillContext context = inAnotherRegion(new SkillContext(caster, trigger, null));
        assertSame(caster, only(registry.resolveAll("", Map.of(), context)));
    }

    @Test
    void targetLocationOfATriggerInAnotherRegionHasNoTarget() {
        SkillContext context = inAnotherRegion(new SkillContext(caster, trigger, null));
        assertTrue(registry.resolveAll("targetlocation", Map.of(), context).isEmpty());
    }

    @Test
    void withoutATriggerTheCasterIsTheTarget() {
        assertSame(caster, only(registry.resolveAll("trigger", Map.of(), SkillContext.of(caster))));
    }
}
