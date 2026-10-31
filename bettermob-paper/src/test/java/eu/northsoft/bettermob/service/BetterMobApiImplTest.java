package eu.northsoft.bettermob.service;

import eu.northsoft.bettermob.BetterMobPlugin;
import eu.northsoft.bettermob.lang.Messages;
import eu.northsoft.bettermob.mob.MobDefinition;
import eu.northsoft.bettermob.mob.MobManager;
import eu.northsoft.bettermob.mob.MobRegistry;
import eu.northsoft.bettermob.skill.SkillEngine;
import eu.northsoft.bettermob.skill.SkillRegistry;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class BetterMobApiImplTest {
    @Test
    void spawnReturnsEmptyAndWarnsWhenLocationIsNotOwnedByCurrentRegion() {
        BetterMobPlugin plugin = mock(BetterMobPlugin.class);
        Messages messages = mock(Messages.class);
        when(plugin.messages()).thenReturn(messages);

        MobManager manager = mock(MobManager.class);
        SkillRegistry skillRegistry = mock(SkillRegistry.class);
        SkillEngine skillEngine = mock(SkillEngine.class);
        BetterMobApiImpl api = new BetterMobApiImpl(plugin, manager, skillRegistry, skillEngine);

        Location location = new Location(mock(World.class), 10, 64, 10);
        try (MockedStatic<Bukkit> bukkit = mockStatic(Bukkit.class)) {
            bukkit.when(() -> Bukkit.isOwnedByCurrentRegion(location)).thenReturn(false);

            Optional<LivingEntity> result = api.spawn("zombie_brute", location);

            assertTrue(result.isEmpty());
            verify(manager, never()).spawn(any(), any());
            verify(messages, times(1)).warnOnce("api.spawnWrongThread", "mob", "zombie_brute");
        }
    }

    @Test
    void spawnSpawnsMobWhenLocationIsOwnedByCurrentRegion() {
        BetterMobPlugin plugin = mock(BetterMobPlugin.class);
        Messages messages = mock(Messages.class);
        when(plugin.messages()).thenReturn(messages);

        MobManager manager = mock(MobManager.class);
        MobRegistry registry = mock(MobRegistry.class);
        when(manager.registry()).thenReturn(registry);

        MobDefinition definition = new MobDefinition("zombie_brute", "ZOMBIE", "Zombie Brute", null,
                20.0, 2.0, false, List.of(), List.of(), null, false, Map.of(),
                List.of(), null, null, null, List.of(), null, null, null);
        when(registry.get("zombie_brute")).thenReturn(definition);

        LivingEntity entity = mock(LivingEntity.class);
        Location location = new Location(mock(World.class), 10, 64, 10);
        when(manager.spawn(definition, location)).thenReturn(entity);

        SkillRegistry skillRegistry = mock(SkillRegistry.class);
        SkillEngine skillEngine = mock(SkillEngine.class);
        BetterMobApiImpl api = new BetterMobApiImpl(plugin, manager, skillRegistry, skillEngine);

        try (MockedStatic<Bukkit> bukkit = mockStatic(Bukkit.class)) {
            bukkit.when(() -> Bukkit.isOwnedByCurrentRegion(location)).thenReturn(true);

            Optional<LivingEntity> result = api.spawn("zombie_brute", location);

            assertTrue(result.isPresent());
            assertSame(entity, result.get());
            verify(manager, times(1)).spawn(definition, location);
            verify(messages, never()).warnOnce(any(), any());
        }
    }

    @Test
    void spawnReturnsEmptyWhenLocationOrWorldIsNull() {
        BetterMobPlugin plugin = mock(BetterMobPlugin.class);
        MobManager manager = mock(MobManager.class);
        SkillRegistry skillRegistry = mock(SkillRegistry.class);
        SkillEngine skillEngine = mock(SkillEngine.class);
        BetterMobApiImpl api = new BetterMobApiImpl(plugin, manager, skillRegistry, skillEngine);

        assertTrue(api.spawn("zombie_brute", null).isEmpty());
        assertTrue(api.spawn("zombie_brute", new Location(null, 0, 0, 0)).isEmpty());
        verify(manager, never()).spawn(any(), any());
    }
}
