package eu.northsoft.bettermob.model;

import org.bukkit.entity.Entity;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Proxy;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BetterModelHookTest {
    private final BetterModelHook hook = new BetterModelHook(null);

    @Test
    void dismountClearsOnlyTheRequestedRidersPluginRegistration() {
        UUID first = UUID.randomUUID();
        UUID second = UUID.randomUUID();
        Registry registry = new Registry(new HashMap<>());
        registry.mountedHitBox().put(first, new MountedHitBox(registry, first));
        registry.mountedHitBox().put(second, new MountedHitBox(registry, second));
        Tracker tracker = new Tracker(registry);

        assertTrue(hook.dismount(tracker, rider(first)));
        assertEquals(1, registry.mountedHitBox().size());
        assertTrue(registry.mountedHitBox().containsKey(second));
        assertFalse(hook.dismount(tracker, rider(first)));
    }

    @Test
    void missingTrackersRegistriesAndRidersAreHarmless() {
        Entity rider = rider(UUID.randomUUID());
        assertFalse(hook.dismount(null, rider));
        assertFalse(hook.dismount(new Tracker(null), rider));
        assertFalse(hook.dismount(new Tracker(new Registry(new HashMap<>())), rider));
    }

    private static Entity rider(UUID id) {
        return (Entity) Proxy.newProxyInstance(Entity.class.getClassLoader(), new Class<?>[]{Entity.class},
                (proxy, method, args) -> {
                    if (method.getName().equals("getUniqueId")) return id;
                    throw new AssertionError("Unexpected Bukkit call: " + method.getName());
                });
    }

    public record Tracker(Registry registry) {}

    public record Registry(Map<UUID, MountedHitBox> mountedHitBox) {}

    public record MountedHitBox(Registry registry, UUID rider) {
        public void dismount() {
            registry.mountedHitBox().remove(rider);
        }
    }
}
