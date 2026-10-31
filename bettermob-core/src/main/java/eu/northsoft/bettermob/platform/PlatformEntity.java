package eu.northsoft.bettermob.platform;

import java.util.UUID;

/** A server entity, wrapped by the platform. */
public interface PlatformEntity {
    UUID uuid();

    /** Namespaced type key, e.g. {@code minecraft:zombie}. */
    String typeKey();

    Position position();

    PlatformWorld world();

    /** {@code true} while the entity exists in a loaded world. */
    boolean isValid();

    void teleport(Position position);

    Vec3 velocity();

    void velocity(Vec3 velocity);

    void remove();

    default String customName() {
        return null;
    }

    default void customName(String name) {}

    default boolean isCustomNameVisible() {
        return false;
    }

    default void customNameVisible(boolean visible) {}

    /** The underlying platform object (Bukkit {@code Entity}, Minecraft {@code Entity}, ...). */
    Object handle();
}
