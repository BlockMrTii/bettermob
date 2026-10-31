package eu.northsoft.bettermob.platform;

import java.nio.file.Path;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.logging.Logger;

/** Entry point into the running server. One instance per platform (Paper plugin / Fabric mod). */
public interface Platform {
    /** e.g. {@code paper}, {@code folia}, {@code fabric}. */
    String name();

    /** Minecraft version, e.g. {@code 1.21.11}. */
    String minecraftVersion();

    Logger logger();

    /** Folder for BetterMob's config, packs and data. */
    Path dataFolder();

    PlatformScheduler scheduler();

    List<PlatformWorld> worlds();

    Optional<PlatformWorld> world(String key);

    Optional<PlatformEntity> entity(UUID uuid);

    /** Registered model backends, in priority order. */
    List<ModelService> models();

    default Optional<ModelService> model(String id) {
        return models().stream().filter(service -> service.id().equalsIgnoreCase(id)).findFirst();
    }
}
