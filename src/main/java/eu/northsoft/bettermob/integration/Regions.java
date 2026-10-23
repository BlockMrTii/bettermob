package eu.northsoft.bettermob.integration;

import eu.northsoft.bettermob.BetterMobPlugin;
import eu.northsoft.bettermob.integration.worldguard.WorldGuardBackend;
import org.bukkit.Bukkit;
import org.bukkit.Location;

import java.util.Set;

public final class Regions {
    public interface Backend {
        Set<String> at(Location location);

        boolean spawnAllowed(Location location);
    }

    private final BetterMobPlugin plugin;
    private volatile Backend backend;
    private volatile boolean tried;

    public Regions(BetterMobPlugin plugin) {
        this.plugin = plugin;
    }

    public static void registerFlags(BetterMobPlugin plugin) {
        if (Bukkit.getPluginManager().getPlugin("WorldGuard") == null) return;
        try {
            WorldGuardBackend.registerFlags();
        } catch (LinkageError | RuntimeException exception) {
            plugin.getLogger().warning("WorldGuard flag could not be registered: " + exception);
        }
    }

    public boolean available() {
        return backend() != null;
    }

    public Set<String> at(Location location) {
        Backend found = backend();
        return found == null ? Set.of() : found.at(location);
    }

    public boolean spawnAllowed(Location location) {
        Backend found = backend();
        return found == null || found.spawnAllowed(location);
    }

    private Backend backend() {
        if (!tried) {
            synchronized (this) {
                if (!tried) {
                    tried = true;
                    if (Bukkit.getPluginManager().isPluginEnabled("WorldGuard")) {
                        try {
                            backend = new WorldGuardBackend();
                            plugin.messages().info("worldguard.enabled");
                        } catch (LinkageError | RuntimeException exception) {
                            plugin.messages().warn("worldguard.failed", "error", exception);
                        }
                    }
                }
            }
        }
        return backend;
    }
}
