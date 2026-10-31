package eu.northsoft.bettermob.fabric;

import eu.northsoft.bettermob.platform.ModelService;
import eu.northsoft.bettermob.platform.Platform;
import eu.northsoft.bettermob.platform.PlatformEntity;
import eu.northsoft.bettermob.platform.PlatformScheduler;
import eu.northsoft.bettermob.platform.PlatformWorld;
import net.fabricmc.loader.api.FabricLoader;

import java.nio.file.Path;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.logging.Logger;

public final class FabricPlatform implements Platform {
    private final Logger logger = Logger.getLogger("BetterMob");
    private final FabricScheduler scheduler = new FabricScheduler();
    private final List<ModelService> models = List.of(new FabricBetterModelService());

    @Override
    public String name() {
        return "fabric";
    }

    @Override
    public String minecraftVersion() {
        return FabricLoader.getInstance().getModContainer("minecraft")
                .map(mod -> mod.getMetadata().getVersion().getFriendlyString())
                .orElse("1.21.11");
    }

    @Override
    public Logger logger() {
        return logger;
    }

    @Override
    public Path dataFolder() {
        return FabricLoader.getInstance().getConfigDir().resolve("bettermob");
    }

    @Override
    public PlatformScheduler scheduler() {
        return scheduler;
    }

    @Override
    public List<PlatformWorld> worlds() {
        return Collections.emptyList();
    }

    @Override
    public Optional<PlatformWorld> world(String key) {
        return Optional.empty();
    }

    @Override
    public Optional<PlatformEntity> entity(UUID uuid) {
        return Optional.empty();
    }

    @Override
    public List<ModelService> models() {
        return models;
    }
}
