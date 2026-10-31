package eu.northsoft.bettermob.platform;

import java.util.Collection;
import java.util.Optional;
import java.util.UUID;

/** A loaded world. */
public interface PlatformWorld {
    /** World key, matches {@link Position#world()}. */
    String key();

    /** Time of day in ticks (0..23999). */
    long time();

    /** Spawns a vanilla entity of the given type key; empty if the type is unknown or spawning failed. */
    Optional<PlatformEntity> spawn(String typeKey, Position position);

    Optional<PlatformEntity> entity(UUID uuid);

    Collection<PlatformEntity> nearbyEntities(Position center, double radius);

    /** Namespaced block key at the position, e.g. {@code minecraft:stone}. */
    String blockKey(int x, int y, int z);

    default void playSound(Position position, String soundKey, float volume, float pitch) {}

    default void strikeLightning(Position position, boolean damage) {}

    default Optional<PlatformEntity> nearestPlayer(Position center, double maxDistance) {
        return Optional.empty();
    }

    Object handle();
}
