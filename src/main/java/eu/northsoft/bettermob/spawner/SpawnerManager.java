package eu.northsoft.bettermob.spawner;

import eu.northsoft.bettermob.BetterMobPlugin;
import eu.northsoft.bettermob.mob.MobDefinition;
import eu.northsoft.bettermob.mob.MobManager;
import eu.northsoft.bettermob.util.Tasks;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.LivingEntity;

import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadLocalRandom;

public final class SpawnerManager {
    private static final long TICK_PERIOD = 20L;
    private static final int PLACEMENT_ATTEMPTS = 8;
    private static final int GROUND_SEARCH_DEPTH = 3;

    private final BetterMobPlugin plugin;
    private final MobManager manager;
    private final File file;
    private final Map<String, Spawner> spawners = new ConcurrentHashMap<>();
    private Runnable cancelTask = () -> { };

    public SpawnerManager(BetterMobPlugin plugin, MobManager manager) {
        this.plugin = plugin;
        this.manager = manager;
        this.file = new File(plugin.getDataFolder(), "spawners.yml");
    }

    public void load() {
        spawners.clear();
        if (!file.exists()) return;
        YamlConfiguration yaml = YamlConfiguration.loadConfiguration(file);
        for (String id : yaml.getKeys(false)) {
            ConfigurationSection section = yaml.getConfigurationSection(id);
            Spawner spawner = section == null ? null : Spawner.read(id, section);
            if (spawner == null) plugin.messages().warn("spawner.invalid", "id", id);
            else spawners.put(key(id), spawner);
        }
    }

    public void start() {
        cancelTask.run();
        cancelTask = Tasks.runGlobalTimer(plugin, TICK_PERIOD, this::tick);
    }

    public void stop() {
        cancelTask.run();
    }

    public Collection<Spawner> all() {
        return spawners.values();
    }

    public synchronized boolean add(Spawner spawner) {
        if (spawners.putIfAbsent(key(spawner.id), spawner) != null) return false;
        save();
        return true;
    }

    public synchronized boolean remove(String id) {
        if (spawners.remove(key(id)) == null) return false;
        save();
        return true;
    }

    private static String key(String id) {
        return id.toLowerCase(Locale.ROOT);
    }

    private synchronized void save() {
        YamlConfiguration yaml = new YamlConfiguration();
        spawners.values().forEach(spawner -> spawner.write(yaml.createSection(spawner.id)));
        try {
            yaml.save(file);
        } catch (IOException exception) {
            plugin.messages().warn("spawner.saveFailed", "error", exception.getMessage());
        }
    }

    private void tick() {
        for (Spawner spawner : spawners.values()) {
            World world = plugin.getServer().getWorld(spawner.world);
            if (world == null) continue;
            Location center = new Location(world, spawner.x, spawner.y, spawner.z);
            Tasks.runAt(plugin, center, () -> tick(spawner, center));
        }
    }

    private void tick(Spawner spawner, Location center) {
        long now = System.currentTimeMillis();
        if (now < spawner.nextSpawnAt || !center.getWorld().isChunkLoaded(center.getBlockX() >> 4, center.getBlockZ() >> 4)) return;
        if (center.getWorld().getNearbyPlayers(center, spawner.playerRange).isEmpty()) return;
        MobDefinition definition = manager.registry().get(spawner.mob);
        if (definition == null) return;
        int alive = 0;
        for (LivingEntity entity : center.getWorld().getNearbyLivingEntities(center, spawner.playerRange + spawner.radius)) {
            if (entity.getScoreboardTags().contains(spawner.tag())) alive++;
        }
        if (!spawner.ready(now, alive)) return;
        Location spot = findSpot(center, spawner.radius);
        if (spot == null) return;
        spawner.nextSpawnAt = now + spawner.intervalSeconds * 1000L;
        manager.spawn(definition, spot).addScoreboardTag(spawner.tag());
    }

    private static Location findSpot(Location center, int radius) {
        ThreadLocalRandom random = ThreadLocalRandom.current();
        for (int attempt = 0; attempt < PLACEMENT_ATTEMPTS; attempt++) {
            double angle = random.nextDouble(Math.PI * 2);
            double distance = radius * Math.sqrt(random.nextDouble());
            Location candidate = center.clone().add(Math.cos(angle) * distance, 0, Math.sin(angle) * distance);
            for (int drop = 0; drop <= GROUND_SEARCH_DEPTH; drop++) {
                Block feet = candidate.getBlock().getRelative(0, -drop, 0);
                if (feet.isPassable() && feet.getRelative(0, 1, 0).isPassable() && !feet.getRelative(0, -1, 0).isPassable()) {
                    return feet.getLocation().add(0.5, 0, 0.5);
                }
            }
        }
        return null;
    }
}
