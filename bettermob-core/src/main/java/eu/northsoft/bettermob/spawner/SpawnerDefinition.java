package eu.northsoft.bettermob.spawner;

import eu.northsoft.bettermob.platform.ConfigSection;

public record SpawnerDefinition(
        String id,
        String mob,
        String world,
        double x,
        double y,
        double z,
        int radius,
        int intervalSeconds,
        int max,
        int playerRange
) {
    public static final int DEFAULT_RADIUS = 5;
    public static final int DEFAULT_INTERVAL_SECONDS = 30;
    public static final int DEFAULT_MAX = 3;
    public static final int DEFAULT_PLAYER_RANGE = 32;

    public static SpawnerDefinition read(String id, ConfigSection section) {
        if (section == null) return null;
        String mob = section.getString("mob");
        String world = section.getString("world");
        if (mob == null || world == null) return null;
        return new SpawnerDefinition(
                id,
                mob,
                world,
                section.getDouble("x", 0.0),
                section.getDouble("y", 0.0),
                section.getDouble("z", 0.0),
                section.getInt("radius", DEFAULT_RADIUS),
                section.getInt("interval", DEFAULT_INTERVAL_SECONDS),
                section.getInt("max", DEFAULT_MAX),
                section.getInt("player-range", DEFAULT_PLAYER_RANGE)
        );
    }
}
