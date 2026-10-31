package eu.northsoft.bettermob.mob;

import eu.northsoft.bettermob.BetterMobPlugin;
import eu.northsoft.bettermob.debug.DebugManager;
import eu.northsoft.bettermob.item.ItemRegistry;
import eu.northsoft.bettermob.model.BetterModelHook;
import eu.northsoft.bettermob.model.ModelEngineHook;
import eu.northsoft.bettermob.skill.SkillContext;
import eu.northsoft.bettermob.skill.SkillEngine;
import org.bukkit.Bukkit;
import org.bukkit.entity.Arrow;
import org.bukkit.entity.LivingEntity;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.MockedStatic;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class RemoteTriggerTest {
    private final LivingEntity mob = mock(LivingEntity.class);
    private final LivingEntity attacker = mock(LivingEntity.class);
    private SkillEngine engine;
    private MobManager manager;

    @BeforeEach
    void setup() {
        BetterMobPlugin plugin = mock(BetterMobPlugin.class);
        when(plugin.getName()).thenReturn("BetterMob");
        when(plugin.namespace()).thenReturn("bettermob");
        DebugManager debug = mock(DebugManager.class);
        when(plugin.debug()).thenReturn(debug);
        when(debug.enter(any())).thenReturn((Runnable) () -> {});
        manager = new MobManager(plugin, mock(MobRegistry.class), mock(BetterModelHook.class), mock(ModelEngineHook.class), mock(ItemRegistry.class));
        engine = mock(SkillEngine.class);
        manager.setSkillEngine(engine);
        when(mob.getUniqueId()).thenReturn(UUID.randomUUID());
    }

    private static MobDefinition definition(boolean threatTable) {
        return new MobDefinition("boss", null, "Boss", null, 100, 1, false, List.of(), List.of(), MobDefinition.Options.DEFAULT,
                threatTable, Map.of(), List.of(MobDefinition.SkillTrigger.parse("ignite @trigger ~onDamaged")), null, null, null,
                List.of(), null, null, null);
    }

    private SkillContext contextOfDamagedSkill(boolean attackerOwned) {
        try (MockedStatic<Bukkit> bukkit = mockStatic(Bukkit.class)) {
            bukkit.when(() -> Bukkit.isOwnedByCurrentRegion(mob)).thenReturn(true);
            bukkit.when(() -> Bukkit.isOwnedByCurrentRegion(attacker)).thenReturn(attackerOwned);
            manager.fireTrigger(mob, definition(false), MobDefinition.SkillTrigger.Trigger.DAMAGED, attacker, null);
        }
        ArgumentCaptor<SkillContext> context = ArgumentCaptor.forClass(SkillContext.class);
        verify(engine).runStep(any(), context.capture());
        return context.getValue();
    }

    @Test
    void aTriggerOfAnotherRegionReachesTheSkillAsRemoteTrigger() {
        SkillContext context = contextOfDamagedSkill(false);
        assertNull(context.trigger());
        assertSame(attacker, context.remoteTrigger());
    }

    @Test
    void anOwnedTriggerReachesTheSkillAsTrigger() {
        SkillContext context = contextOfDamagedSkill(true);
        assertSame(attacker, context.trigger());
        assertNull(context.remoteTrigger());
    }

    @Test
    void aurasOfEntitiesWithoutDefinitionGetATriggerOfAnotherRegion() {
        try (MockedStatic<Bukkit> bukkit = mockStatic(Bukkit.class)) {
            bukkit.when(() -> Bukkit.isOwnedByCurrentRegion(mob)).thenReturn(true);
            bukkit.when(() -> Bukkit.isOwnedByCurrentRegion(attacker)).thenReturn(false);
            manager.fireEntityAura(mob, "ondamaged", attacker, null);
        }
        verify(engine).fireAuras(mob, "ondamaged", attacker, null);
    }

    private static EntityDamageByEntityEvent arrowHit(LivingEntity victim, LivingEntity shooter) {
        Arrow arrow = mock(Arrow.class);
        when(arrow.getShooter()).thenReturn(shooter);
        EntityDamageByEntityEvent event = mock(EntityDamageByEntityEvent.class);
        when(event.getDamager()).thenReturn(arrow);
        when(event.getEntity()).thenReturn(victim);
        when(event.getFinalDamage()).thenReturn(3.0);
        return event;
    }

    @Test
    void aShooterOfAnotherRegionTriggersOnDamagedButAddsNoThreatAndAlertsNoGroup() {
        MobManager mobs = mock(MobManager.class);
        MobDefinition definition = definition(true);
        when(mobs.definitionOf(any())).thenReturn(definition);
        EntityDamageByEntityEvent event = arrowHit(mob, attacker);
        try (MockedStatic<Bukkit> bukkit = mockStatic(Bukkit.class)) {
            bukkit.when(() -> Bukkit.isOwnedByCurrentRegion(attacker)).thenReturn(false);
            new MobListener(mobs, null, null).onDamage(event);
        }
        verify(mobs).fireTrigger(mob, definition, MobDefinition.SkillTrigger.Trigger.DAMAGED, attacker, event);
        verify(mobs, never()).registerThreat(any(), any(), anyDouble());
        verify(mobs, never()).alertGroup(any(), any(), any());
    }

    @Test
    void anOwnedShooterTriggersOnDamagedAddsThreatAndAlertsTheGroup() {
        MobManager mobs = mock(MobManager.class);
        MobDefinition definition = definition(true);
        when(mobs.definitionOf(any())).thenReturn(definition);
        EntityDamageByEntityEvent event = arrowHit(mob, attacker);
        try (MockedStatic<Bukkit> bukkit = mockStatic(Bukkit.class)) {
            bukkit.when(() -> Bukkit.isOwnedByCurrentRegion(attacker)).thenReturn(true);
            new MobListener(mobs, null, null).onDamage(event);
        }
        verify(mobs).fireTrigger(mob, definition, MobDefinition.SkillTrigger.Trigger.DAMAGED, attacker, event);
        verify(mobs).registerThreat(mob.getUniqueId(), attacker, 3.0);
        verify(mobs).alertGroup(mob, definition, attacker);
    }
}
