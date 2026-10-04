package eu.northsoft.bettermob.mob;

import org.bukkit.configuration.ConfigurationSection;

import java.util.List;
import java.util.Locale;
import java.util.function.Consumer;

public record SpawnRule(List<String> worlds, List<String> biomes, Time time, double chance) {
    public enum Time { ANY, DAY, NIGHT }

    public static final long NIGHT_START = 13000;
    public static final long NIGHT_END = 23000;

    public SpawnRule {
        worlds = worlds.stream().map(name -> name.trim().toLowerCase(Locale.ROOT)).toList();
        biomes = biomes.stream().map(SpawnRule::biomeKey).toList();
        chance = Math.max(0, Math.min(1, chance));
    }

    public static SpawnRule parse(ConfigurationSection section, Consumer<String> invalidOption) {
        for (String key : List.of("Worlds", "Biomes")) {
            if (section.contains(key) && !section.isList(key)) {
                invalidOption.accept(key);
                return null;
            }
        }
        if (section.contains("Chance") && !(section.get("Chance") instanceof Number)) {
            invalidOption.accept("Chance");
            return null;
        }
        Time time = Time.ANY;
        if (section.contains("Time")) {
            try {
                time = Time.valueOf(String.valueOf(section.get("Time")).trim().toUpperCase(Locale.ROOT));
            } catch (IllegalArgumentException exception) {
                invalidOption.accept("Time");
                return null;
            }
        }
        return new SpawnRule(section.getStringList("Worlds"), section.getStringList("Biomes"), time, section.getDouble("Chance", 1));
    }

    public boolean matches(String world, String biome, long worldTime, double roll) {
        if (!worlds.isEmpty() && !worlds.contains(world.toLowerCase(Locale.ROOT))) return false;
        if (!biomes.isEmpty() && !biomes.contains(biomeKey(biome))) return false;
        boolean night = worldTime >= NIGHT_START && worldTime < NIGHT_END;
        if (time == Time.NIGHT && !night || time == Time.DAY && night) return false;
        return roll < chance;
    }

    private static String biomeKey(String biome) {
        String key = biome.trim().toLowerCase(Locale.ROOT);
        return key.startsWith("minecraft:") ? key.substring("minecraft:".length()) : key;
    }
}
