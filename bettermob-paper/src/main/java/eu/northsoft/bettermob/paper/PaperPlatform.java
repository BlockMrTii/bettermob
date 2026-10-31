package eu.northsoft.bettermob.paper;

import eu.northsoft.bettermob.platform.ModelService;
import eu.northsoft.bettermob.platform.Platform;
import eu.northsoft.bettermob.platform.PlatformEntity;
import eu.northsoft.bettermob.platform.PlatformScheduler;
import eu.northsoft.bettermob.platform.PlatformWorld;
import eu.northsoft.bettermob.util.Tasks;
import org.bukkit.Bukkit;
import org.bukkit.plugin.Plugin;

import java.nio.file.Path;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.logging.Logger;

/** {@link Platform} implementation for Paper and Folia. */
public final class PaperPlatform implements Platform {
    private final Plugin plugin;
    private final PaperScheduler scheduler;
    private final List<ModelService> models;

    public PaperPlatform(Plugin plugin, List<ModelService> models) {
        this.plugin = plugin;
        this.scheduler = new PaperScheduler(plugin);
        this.models = List.copyOf(models);
    }

    @Override
    public String name() {
        return Tasks.FOLIA ? "folia" : "paper";
    }

    @Override
    public String minecraftVersion() {
        return Bukkit.getMinecraftVersion();
    }

    @Override
    public Logger logger() {
        return plugin.getLogger();
    }

    @Override
    public Path dataFolder() {
        return plugin.getDataFolder().toPath();
    }

    @Override
    public PlatformScheduler scheduler() {
        return scheduler;
    }

    @Override
    public List<PlatformWorld> worlds() {
        return Bukkit.getWorlds().stream().<PlatformWorld>map(PaperWorld::new).toList();
    }

    @Override
    public Optional<PlatformWorld> world(String key) {
        return Optional.ofNullable(Bukkit.getWorld(key)).map(PaperWorld::new);
    }

    @Override
    public Optional<PlatformEntity> entity(UUID uuid) {
        return Optional.ofNullable(PaperAdapters.entity(Bukkit.getEntity(uuid)));
    }

    @Override
    public List<ModelService> models() {
        return models;
    }
}
