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
    private volatile net.minecraft.server.MinecraftServer server;

    public void setServer(net.minecraft.server.MinecraftServer server) {
        this.server = server;
    }

    public net.minecraft.server.MinecraftServer server() {
        return server;
    }

    @Override
    public String name() {
        return "fabric";
    }

    @Override
    public String minecraftVersion() {
        try {
            if (FabricLoader.getInstance() != null) {
                return FabricLoader.getInstance().getModContainer("minecraft")
                        .map(mod -> mod.getMetadata().getVersion().getFriendlyString())
                        .orElse("1.21.11");
            }
        } catch (Throwable ignored) {}
        return "1.21.11";
    }

    @Override
    public Logger logger() {
        return logger;
    }

    @Override
    public Path dataFolder() {
        try {
            if (FabricLoader.getInstance() != null) {
                Path configDir = FabricLoader.getInstance().getConfigDir();
                if (configDir != null) {
                    return configDir.resolve("bettermob");
                }
            }
        } catch (Throwable ignored) {}
        return Path.of("config/bettermob");
    }

    @Override
    public PlatformScheduler scheduler() {
        return scheduler;
    }

    @Override
    public List<PlatformWorld> worlds() {
        if (server == null) return Collections.emptyList();
        var list = new java.util.ArrayList<PlatformWorld>();
        for (var sw : server.getWorlds()) {
            list.add(new FabricWorld(sw));
        }
        return list;
    }

    @Override
    public Optional<PlatformWorld> world(String key) {
        if (server == null || key == null) return Optional.empty();
        for (var sw : server.getWorlds()) {
            if (sw.getRegistryKey().getValue().toString().equalsIgnoreCase(key)) {
                return Optional.of(new FabricWorld(sw));
            }
        }
        return Optional.empty();
    }

    @Override
    public Optional<PlatformEntity> entity(UUID uuid) {
        if (server == null || uuid == null) return Optional.empty();
        for (var sw : server.getWorlds()) {
            var e = sw.getEntity(uuid);
            if (e != null) return Optional.of(FabricAdapters.entity(e));
        }
        return Optional.empty();
    }

    @Override
    public List<ModelService> models() {
        return models;
    }

    @Override
    public void dispatchCommand(String command) {
        if (server != null && command != null && !command.isBlank()) {
            server.getCommandManager().parseAndExecute(server.getCommandSource(), command);
        }
    }
}
