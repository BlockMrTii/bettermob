package eu.northsoft.bettermob.api;

import eu.northsoft.bettermob.api.event.mob.BetterMobDropEvent;
import eu.northsoft.bettermob.api.event.mob.BetterMobGroupAlertEvent;
import eu.northsoft.bettermob.api.event.mob.BetterMobPhaseEvent;
import eu.northsoft.bettermob.api.event.mob.BetterMobTargetEvent;
import eu.northsoft.bettermob.api.event.skill.BetterMobMechanicEvent;
import eu.northsoft.bettermob.api.event.spawner.BetterMobSpawnerSpawnEvent;
import eu.northsoft.bettermob.api.event.system.BetterMobReloadEvent;
import org.bukkit.entity.EntityType;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ApiEventsTest {
    private static final MobInfo INFO = new MobInfo("boss", EntityType.ZOMBIE, "Boss", null, 20, 2);

    @Test
    void theDropEventExposesTheLiveListAndAnEditableExperience() {
        List<org.bukkit.inventory.ItemStack> drops = new ArrayList<>();
        BetterMobDropEvent event = new BetterMobDropEvent(null, INFO, null, drops, 5);
        assertSame(drops, event.getDrops());
        assertNull(event.getKiller());
        event.setExp(12);
        assertEquals(12, event.getExp());
        assertSame(INFO, event.getMob());
    }

    @Test
    void cancellableEventsStartUncancelledAndRemember() {
        BetterMobTargetEvent target = new BetterMobTargetEvent(null, INFO, null, null);
        assertFalse(target.isCancelled());
        target.setCancelled(true);
        assertTrue(target.isCancelled());
        BetterMobSpawnerSpawnEvent spawner = new BetterMobSpawnerSpawnEvent("camp", "goblin", null);
        spawner.setCancelled(true);
        assertTrue(spawner.isCancelled());
        assertEquals("camp", spawner.getSpawnerId());
        assertFalse(new BetterMobGroupAlertEvent(null, INFO, null, List.of()).isCancelled());
        assertFalse(new BetterMobMechanicEvent(null, "damage", Map.of(), null, null).isCancelled());
    }

    @Test
    void phaseAndReloadEventsCarryTheirData() {
        BetterMobPhaseEvent phase = new BetterMobPhaseEvent(null, INFO, -1, 0);
        assertEquals(-1, phase.getFrom());
        assertEquals(0, phase.getTo());
        BetterMobReloadEvent.Changes none = new BetterMobReloadEvent.Changes(List.of(), List.of(), List.of());
        BetterMobReloadEvent reload = new BetterMobReloadEvent(Map.of("mobs", none, "skills", new BetterMobReloadEvent.Changes(List.of("x"), List.of(), List.of())), 3);
        assertTrue(reload.getChanges().get("mobs").isEmpty());
        assertFalse(reload.getChanges().get("skills").isEmpty());
        assertEquals(3, reload.getUpdatedMobs());
    }

    @Test
    void everyEventHasItsOwnHandlerList() {
        assertNotNull(BetterMobDropEvent.getHandlerList());
        assertFalse(BetterMobDropEvent.getHandlerList() == BetterMobPhaseEvent.getHandlerList());
        assertSame(BetterMobDropEvent.getHandlerList(), new BetterMobDropEvent(null, INFO, null, List.of(), 0).getHandlers());
    }
}
