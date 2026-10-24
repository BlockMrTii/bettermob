package eu.northsoft.bettermob.mob;

import eu.northsoft.bettermob.BetterMobPlugin;
import eu.northsoft.bettermob.util.RegionEntities;
import eu.northsoft.bettermob.util.Tasks;
import org.bukkit.GameMode;
import org.bukkit.HeightMap;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;

import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadLocalRandom;

/** Creates independent, land-based spawn attempts around active players. */
public final class RandomSpawnManager {
    private static final long PERIOD_TICKS = 400;
    private static final int MIN_DISTANCE = 24;
    private static final int MAX_DISTANCE = 48;
    private static final int NEARBY_RADIUS = 48;

    private final BetterMobPlugin plugin;
    private final MobManager manager;
    private final Map<Key, Long> nextSpawnAt = new ConcurrentHashMap<>();
    private Runnable cancelTask = () -> { };

    public RandomSpawnManager(BetterMobPlugin plugin, MobManager manager) {
        this.plugin = plugin;
        this.manager = manager;
    }

    public void start() {
        stop();
        reportRuleCount();
        cancelTask = Tasks.runGlobalTimer(plugin, PERIOD_TICKS, this::tick);
    }

    public void reload() {
        nextSpawnAt.clear();
        reportRuleCount();
    }

    public void stop() {
        cancelTask.run();
        cancelTask = () -> { };
        nextSpawnAt.clear();
    }

    private void tick() {
        for (Player player : plugin.getServer().getOnlinePlayers()) {
            Tasks.runOn(plugin, player, () -> attemptFor(player));
        }
    }

    private void attemptFor(Player player) {
        if (!player.isOnline() || player.isDead()) return;
        Location center = player.getLocation();
        UUID playerId = player.getUniqueId();
        for (MobDefinition definition : manager.registry().all().values()) {
            SpawnRule rule = definition.spawnRule;
            if (rule == null || rule.action() != SpawnRule.Action.ADD) continue;
            Key key = new Key(playerId, definition.id);
            if (player.getGameMode() != GameMode.SURVIVAL && player.getGameMode() != GameMode.ADVENTURE) {
                report(key, center, "player must be in Survival or Adventure mode");
                continue;
            }
            long now = System.currentTimeMillis();
            if (now < nextSpawnAt.getOrDefault(key, 0L)) continue;
            Location candidate = randomPoint(center);
            World world = candidate.getWorld();
            int chunkX = candidate.getBlockX() >> 4;
            int chunkZ = candidate.getBlockZ() >> 4;
            if (!world.isChunkLoaded(chunkX, chunkZ)) {
                report(key, candidate, "chunk not loaded");
                continue;
            }
            Tasks.runAt(plugin, candidate, () -> attemptAt(key, definition.id, rule, candidate));
        }
    }

    private void attemptAt(Key key, String mobId, SpawnRule rule, Location candidate) {
        MobDefinition definition = manager.registry().get(mobId);
        if (definition == null || definition.spawnRule != rule) return;
        World world = candidate.getWorld();
        int x = candidate.getBlockX();
        int z = candidate.getBlockZ();
        if (!world.isChunkLoaded(x >> 4, z >> 4)) {
            report(key, candidate, "chunk not loaded");
            return;
        }
        int y = world.getHighestBlockYAt(x, z, HeightMap.MOTION_BLOCKING_NO_LEAVES);
        if (y < world.getMinHeight() || y + 2 >= world.getMaxHeight()) {
            report(key, candidate, "outside build height");
            return;
        }
        Block ground = world.getBlockAt(x, y, z);
        Block feet = world.getBlockAt(x, y + 1, z);
        if (!ground.getType().isSolid() || !feet.isPassable() || !feet.getRelative(0, 1, 0).isPassable()) {
            report(key, candidate, "no safe ground");
            return;
        }
        Location spot = new Location(world, x + 0.5, y + 1, z + 0.5);
        String biome = world.getBiome(spot).getKey().toString();
        Set<String> regionsHere = rule.usesRegions() ? manager.regions().at(spot) : Set.of();
        if (!rule.matchesLocation(world.getName(), biome, world.getTime(), regionsHere)) {
            report(key, spot, "world, biome, time or region excluded");
            return;
        }
        if (!manager.regions().spawnAllowed(spot)) {
            report(key, spot, "region prevents spawning");
            return;
        }
        if (!rule.matches(world.getName(), biome, world.getTime(), ThreadLocalRandom.current().nextDouble(), regionsHere)) {
            report(key, spot, "chance missed");
            return;
        }
        int nearby = 0;
        for (Entity entity : RegionEntities.near(spot, NEARBY_RADIUS)) {
            MobDefinition found = manager.definitionOf(entity.getUniqueId());
            if (found != null && found.id.equalsIgnoreCase(mobId) && ++nearby >= rule.maxNearby()) {
                report(key, spot, "MaxNearby reached");
                return;
            }
        }
        try {
            if (manager.spawn(definition, spot) != null) {
                nextSpawnAt.put(key, System.currentTimeMillis() + rule.cooldown() * 1000L);
                report(key, spot, "spawned");
            }
        } catch (RuntimeException exception) {
            plugin.getLogger().warning("Random spawn failed for " + mobId + ": " + exception.getMessage());
            report(key, spot, "spawn failed: " + exception.getMessage());
        }
    }

    private void report(Key key, Location at, String result) {
        plugin.debug().randomSpawn(key.player(), key.mob(),
                "ADD " + key.mob() + " at " + at.getWorld().getName() + " "
                        + at.getBlockX() + " " + at.getBlockY() + " " + at.getBlockZ() + ": " + result);
    }

    private void reportRuleCount() {
        var ids = manager.registry().all().values().stream()
                .filter(definition -> definition.spawnRule != null && definition.spawnRule.action() == SpawnRule.Action.ADD)
                .map(definition -> definition.id)
                .toList();
        plugin.getLogger().info(ids.size() + " ADD spawn rule(s) active" + (ids.isEmpty() ? "." : ": " + String.join(", ", ids)));
    }

    private static Location randomPoint(Location center) {
        ThreadLocalRandom random = ThreadLocalRandom.current();
        double angle = random.nextDouble(Math.PI * 2);
        double distance = random.nextDouble(MIN_DISTANCE, MAX_DISTANCE);
        return center.clone().add(Math.cos(angle) * distance, 0, Math.sin(angle) * distance);
    }

    private record Key(UUID player, String mob) { }
}
