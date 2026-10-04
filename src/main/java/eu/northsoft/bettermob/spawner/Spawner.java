package eu.northsoft.bettermob.spawner;

import org.bukkit.configuration.ConfigurationSection;

import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class Spawner {
    private static final String TAG_PREFIX = "bettermob_spawner:";

    public static final int DEFAULT_RADIUS = 5;
    public static final int DEFAULT_INTERVAL_SECONDS = 30;
    public static final int DEFAULT_MAX = 3;
    public static final int DEFAULT_PLAYER_RANGE = 32;

    public final String id;
    public final String mob;
    public final String world;
    public final double x;
    public final double y;
    public final double z;
    public final int radius;
    public final int intervalSeconds;
    public final int max;
    public final int playerRange;
    final Set<UUID> alive = ConcurrentHashMap.newKeySet();
    volatile long nextSpawnAt;

    public Spawner(String id, String mob, String world, double x, double y, double z, int radius, int intervalSeconds, int max, int playerRange) {
        if (id.isEmpty() || id.indexOf('.') >= 0) throw new IllegalArgumentException("Invalid spawner id: " + id);
        this.id = id;
        this.mob = mob;
        this.world = world;
        this.x = x;
        this.y = y;
        this.z = z;
        this.radius = Math.max(0, radius);
        this.intervalSeconds = Math.max(1, intervalSeconds);
        this.max = Math.max(1, max);
        this.playerRange = Math.max(1, playerRange);
    }

    public static Spawner read(String id, ConfigurationSection section) {
        String mob = section.getString("mob");
        String world = section.getString("world");
        if (mob == null || world == null) return null;
        return new Spawner(id, mob, world, section.getDouble("x"), section.getDouble("y"), section.getDouble("z"),
                section.getInt("radius", DEFAULT_RADIUS), section.getInt("interval", DEFAULT_INTERVAL_SECONDS),
                section.getInt("max", DEFAULT_MAX), section.getInt("player-range", DEFAULT_PLAYER_RANGE));
    }

    public void write(ConfigurationSection section) {
        section.set("mob", mob);
        section.set("world", world);
        section.set("x", x);
        section.set("y", y);
        section.set("z", z);
        section.set("radius", radius);
        section.set("interval", intervalSeconds);
        section.set("max", max);
        section.set("player-range", playerRange);
    }

    public String tag() {
        return TAG_PREFIX + id;
    }

    public static String idOfTag(String tag) {
        return tag.startsWith(TAG_PREFIX) ? tag.substring(TAG_PREFIX.length()) : null;
    }

    boolean ready(long now, int alive) {
        return now >= nextSpawnAt && alive < max;
    }
}
