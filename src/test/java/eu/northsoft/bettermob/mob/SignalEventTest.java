package eu.northsoft.bettermob.mob;

import eu.northsoft.bettermob.BetterMobPlugin;
import eu.northsoft.bettermob.api.event.mob.BetterMobSignalEvent;
import eu.northsoft.bettermob.debug.DebugManager;
import eu.northsoft.bettermob.item.ItemRegistry;
import eu.northsoft.bettermob.model.BetterModelHook;
import eu.northsoft.bettermob.model.ModelEngineHook;
import org.bukkit.Bukkit;
import org.bukkit.entity.LivingEntity;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.RegisteredListener;
import org.junit.jupiter.api.AfterEach;
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

class SignalEventTest {
    private final LivingEntity mob = mock(LivingEntity.class);
    private final LivingEntity sender = mock(LivingEntity.class);
    private final RegisteredListener listening = new RegisteredListener(new Listener() { }, (listener, event) -> { },
            EventPriority.NORMAL, mock(Plugin.class), false);
    private MobManager manager;

    @BeforeEach
    void setup() {
        BetterMobPlugin plugin = mock(BetterMobPlugin.class);
        when(plugin.getName()).thenReturn("BetterMob");
        when(plugin.namespace()).thenReturn("bettermob");
        DebugManager debug = mock(DebugManager.class);
        when(plugin.debug()).thenReturn(debug);
        when(debug.enter(any())).thenReturn((Runnable) () -> {});
        MobRegistry registry = mock(MobRegistry.class);
        when(registry.get("boss")).thenReturn(new MobDefinition("boss", null, "Boss", null, 100, 1, false, List.of(), List.of(),
                MobDefinition.Options.DEFAULT, false, Map.of(), List.of(MobDefinition.SkillTrigger.parse("ignite @trigger ~onSignal:rage")),
                null, null, null, List.of(), null, null, null));
        manager = new MobManager(plugin, registry, mock(BetterModelHook.class), mock(ModelEngineHook.class), mock(ItemRegistry.class));
        when(mob.getUniqueId()).thenReturn(UUID.randomUUID());
        PersistentDataContainer data = mock(PersistentDataContainer.class);
        when(mob.getPersistentDataContainer()).thenReturn(data);
        when(data.get(manager.mobIdKey, PersistentDataType.STRING)).thenReturn("boss");
        manager.handleLoad(mob);
        BetterMobSignalEvent.getHandlerList().register(listening);
    }

    @AfterEach
    void stopListening() {
        BetterMobSignalEvent.getHandlerList().unregister(listening);
    }

    private BetterMobSignalEvent signalFrom(boolean senderOwned) {
        PluginManager plugins = mock(PluginManager.class);
        try (MockedStatic<Bukkit> bukkit = mockStatic(Bukkit.class)) {
            bukkit.when(Bukkit::getPluginManager).thenReturn(plugins);
            bukkit.when(() -> Bukkit.isOwnedByCurrentRegion(sender)).thenReturn(senderOwned);
            assertTrue(manager.signal(mob, "rage", sender));
        }
        ArgumentCaptor<BetterMobSignalEvent> event = ArgumentCaptor.forClass(BetterMobSignalEvent.class);
        verify(plugins).callEvent(event.capture());
        return event.getValue();
    }

    @Test
    void theSignalEventLeavesOutASenderOfAnotherRegion() {
        assertNull(signalFrom(false).getSender());
    }

    @Test
    void theSignalEventHandsOverAnOwnedSender() {
        assertSame(sender, signalFrom(true).getSender());
    }
}
