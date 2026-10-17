package eu.northsoft.bettermob.spawner;

import eu.northsoft.bettermob.BetterMobPlugin;
import eu.northsoft.bettermob.mob.MobDefinition;
import eu.northsoft.bettermob.mob.MobManager;
import eu.northsoft.bettermob.util.Tasks;
import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;

import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadLocalRandom;

public final class SpawnerManager {
    private static final long TICK_PERIOD = 20L;
    private static final long RETRY_MILLIS = 1000L;
    private static final int CHUNK_SIZE = 16;
    private static final long PLAYER_FRESH_MILLIS = 3000L;
    private static final int GROUND_SEARCH_DEPTH = 3;
    private static final int MAX_RECONCILE_ATTEMPTS = 6;

    private final BetterMobPlugin plugin;
    private final MobManager manager;
    private final File file;
    private final Map<String, Spawner> spawners = new ConcurrentHashMap<>();
    private volatile Runnable cancelTask = () -> { };

    public SpawnerManager(BetterMobPlugin plugin, MobManager manager) {
        this.plugin = plugin;
        this.manager = manager;
        this.file = new File(plugin.getDataFolder(), "spawners.yml");
    }

    public synchronized void load() {
        Map<String, Spawner> loaded = new HashMap<>();
        if (file.exists()) {
            YamlConfiguration yaml = YamlConfiguration.loadConfiguration(file);
            for (String id : yaml.getKeys(false)) {
                ConfigurationSection section = yaml.getConfigurationSection(id);
                Spawner spawner = section == null ? null : Spawner.read(id, section);
                if (spawner == null) plugin.messages().warn("spawner.invalid", "id", id);
                else loaded.put(key(id), spawner);
            }
        }
        loaded.forEach((key, spawner) -> {
            Spawner previous = spawners.get(key);
            if (previous != null) spawner.adopt(previous);
        });
        spawners.keySet().retainAll(loaded.keySet());
        spawners.putAll(loaded);
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
        spawner.reconciled = true;
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
        for (Player player : plugin.getServer().getOnlinePlayers()) Tasks.runOn(plugin, player, () -> markNearby(player));
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
        if (!spawner.reconciled) {
            reconcile(spawner, center);
            if (!spawner.reconciled) return;
        }
        if (now - spawner.playerNearAt > PLAYER_FRESH_MILLIS) return;
        MobDefinition definition = manager.registry().get(spawner.mob);
        if (definition == null) return;
        if (!spawner.ready(now, spawner.alive.size())) return;
        Location candidate = randomPoint(center, spawner.radius);
        spawner.nextSpawnAt = now + RETRY_MILLIS;
        Tasks.runAt(plugin, candidate, () -> spawnAt(spawner, definition, candidate));
    }

    private void spawnAt(Spawner spawner, MobDefinition definition, Location candidate) {
        Location spot = groundBelow(candidate);
        if (spot == null) return;
        spawner.nextSpawnAt = System.currentTimeMillis() + spawner.intervalSeconds * 1000L;
        try {
            LivingEntity entity = manager.spawn(definition, spot);
            entity.addScoreboardTag(spawner.tag());
            spawner.alive.add(entity.getUniqueId());
        } catch (RuntimeException exception) {
            plugin.messages().warn("spawner.spawnFailed", "id", spawner.id, "mob", spawner.mob, "error", exception.getMessage());
        }
    }

    private void reconcile(Spawner spawner, Location center) {
        int chunkRadius = spawner.radius / CHUNK_SIZE + 1;
        World world = center.getWorld();
        int centerX = center.getBlockX() >> 4;
        int centerZ = center.getBlockZ() >> 4;
        boolean notLoaded = false;
        boolean foreignRegion = false;
        for (int x = centerX - chunkRadius; x <= centerX + chunkRadius; x++) {
            for (int z = centerZ - chunkRadius; z <= centerZ + chunkRadius; z++) {
                if (!plugin.getServer().isOwnedByCurrentRegion(world, x, z)) {
                    foreignRegion = true;
                    continue;
                }
                if (!world.isChunkLoaded(x, z)) {
                    notLoaded = true;
                    continue;
                }
                Chunk chunk = world.getChunkAt(x, z, false);
                if (chunk == null || !chunk.isEntitiesLoaded()) {
                    notLoaded = true;
                    continue;
                }
                for (Entity entity : chunk.getEntities()) {
                    if (entity.getScoreboardTags().contains(spawner.tag())) spawner.alive.add(entity.getUniqueId());
                }
            }
        }
        if (notLoaded) return;
        if (!foreignRegion || ++spawner.reconcileAttempts >= MAX_RECONCILE_ATTEMPTS) spawner.reconciled = true;
    }

    void track(Entity entity) {
        Spawner spawner = ownerOf(entity);
        if (spawner != null) spawner.alive.add(entity.getUniqueId());
    }

    void untrack(Entity entity) {
        Spawner spawner = ownerOf(entity);
        if (spawner != null) spawner.alive.remove(entity.getUniqueId());
    }

    private Spawner ownerOf(Entity entity) {
        for (String tag : entity.getScoreboardTags()) {
            String id = Spawner.idOfTag(tag);
            if (id != null) return spawners.get(key(id));
        }
        return null;
    }

    private void markNearby(Player player) {
        Location at = player.getLocation();
        String world = at.getWorld().getName();
        long now = System.currentTimeMillis();
        for (Spawner spawner : spawners.values()) {
            if (!spawner.world.equals(world)) continue;
            double dx = at.getX() - spawner.x;
            double dy = at.getY() - spawner.y;
            double dz = at.getZ() - spawner.z;
            if (dx * dx + dy * dy + dz * dz <= (double) spawner.playerRange * spawner.playerRange) spawner.playerNearAt = now;
        }
    }

    private static Location randomPoint(Location center, int radius) {
        ThreadLocalRandom random = ThreadLocalRandom.current();
        double angle = random.nextDouble(Math.PI * 2);
        double distance = radius * Math.sqrt(random.nextDouble());
        return center.clone().add(Math.cos(angle) * distance, 0, Math.sin(angle) * distance);
    }

    private static Location groundBelow(Location candidate) {
        for (int drop = 0; drop <= GROUND_SEARCH_DEPTH; drop++) {
            Block feet = candidate.getBlock().getRelative(0, -drop, 0);
            if (feet.isPassable() && feet.getRelative(0, 1, 0).isPassable() && !feet.getRelative(0, -1, 0).isPassable()) {
                return feet.getLocation().add(0.5, 0, 0.5);
            }
        }
        return null;
    }
}
