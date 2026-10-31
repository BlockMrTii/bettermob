package eu.northsoft.bettermob.skill;

import org.bukkit.Bukkit;
import org.bukkit.entity.LivingEntity;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;

class SkillContextTest {
    private final LivingEntity caster = mock(LivingEntity.class);
    private final LivingEntity trigger = mock(LivingEntity.class);

    @Test
    void aTriggerOfAnotherRegionIsKeptAsRemoteTrigger() {
        try (MockedStatic<Bukkit> bukkit = mockStatic(Bukkit.class)) {
            bukkit.when(() -> Bukkit.isOwnedByCurrentRegion(trigger)).thenReturn(false);
            SkillContext context = SkillContext.of(caster).withTrigger(trigger).ownedHere();
            assertNull(context.trigger());
            assertSame(trigger, context.remoteTrigger());
            assertTrue(context.targetIsTrigger());
        }
    }

    @Test
    void aRemoteTriggerComesBackOnceTheRegionOwnsItAgain() {
        try (MockedStatic<Bukkit> bukkit = mockStatic(Bukkit.class)) {
            bukkit.when(() -> Bukkit.isOwnedByCurrentRegion(trigger)).thenReturn(false);
            SkillContext remote = SkillContext.of(caster).withTrigger(trigger).ownedHere();
            bukkit.when(() -> Bukkit.isOwnedByCurrentRegion(trigger)).thenReturn(true);
            SkillContext owned = remote.ownedHere();
            assertSame(trigger, owned.trigger());
            assertNull(owned.remoteTrigger());
        }
    }

    @Test
    void anOwnedTriggerStaysUnchanged() {
        try (MockedStatic<Bukkit> bukkit = mockStatic(Bukkit.class)) {
            bukkit.when(() -> Bukkit.isOwnedByCurrentRegion(trigger)).thenReturn(true);
            SkillContext context = SkillContext.of(caster).withTrigger(trigger);
            assertSame(context, context.ownedHere());
        }
    }
}
