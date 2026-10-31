package eu.northsoft.bettermob.mob;

import eu.northsoft.bettermob.platform.ConfigSection;

import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.function.Consumer;

public record SpawnRule(List<String> worlds, List<String> biomes, Time time, double chance, List<String> regions,
                        List<String> notRegions, Action action, int cooldown, int maxNearby) {
    public enum Time { ANY, DAY, NIGHT }
    public enum Action { REPLACE, ADD }

    public static final long NIGHT_START = 13000;
    public static final long NIGHT_END = 23000;

    public SpawnRule(List<String> worlds, List<String> biomes, Time time, double chance) {
        this(worlds, biomes, time, chance, List.of(), List.of());
    }

    public SpawnRule(List<String> worlds, List<String> biomes, Time time, double chance,
                     List<String> regions, List<String> notRegions) {
        this(worlds, biomes, time, chance, regions, notRegions, Action.REPLACE, 60, 3);
    }

    public SpawnRule {
        worlds = worlds.stream().map(name -> name.trim().toLowerCase(Locale.ROOT)).toList();
        biomes = biomes.stream().map(SpawnRule::biomeKey).toList();
        chance = Math.max(0, Math.min(1, chance));
        regions = regions.stream().map(name -> name.trim().toLowerCase(Locale.ROOT)).toList();
        notRegions = notRegions.stream().map(name -> name.trim().toLowerCase(Locale.ROOT)).toList();
        cooldown = Math.max(0, cooldown);
        maxNearby = Math.max(1, maxNearby);
    }

    public static SpawnRule parse(ConfigSection section, Consumer<String> invalidOption) {
        if (section == null) return null;
        for (String key : List.of("Worlds", "Biomes", "Regions", "NotRegions")) {
            if (section.contains(key) && !section.isList(key)) {
                invalidOption.accept(key);
                return null;
            }
        }
        if (section.contains("Chance") && !(section.get("Chance") instanceof Number)) {
            invalidOption.accept("Chance");
            return null;
        }
        for (String key : List.of("Cooldown", "MaxNearby")) {
            if (section.contains(key) && !(section.get(key) instanceof Number)) {
                invalidOption.accept(key);
                return null;
            }
        }
        Action action;
        try {
            action = Action.valueOf(section.getString("Action", "REPLACE").trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException exception) {
            invalidOption.accept("Action");
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
        return new SpawnRule(section.getStringList("Worlds"), section.getStringList("Biomes"), time, section.getDouble("Chance", 1),
                section.getStringList("Regions"), section.getStringList("NotRegions"), action,
                section.getInt("Cooldown", 60), section.getInt("MaxNearby", 3));
    }

    public boolean usesRegions() {
        return !regions.isEmpty() || !notRegions.isEmpty();
    }

    public boolean matches(String world, String biome, long worldTime, double roll) {
        return matches(world, biome, worldTime, roll, Set.of());
    }

    public boolean matches(String world, String biome, long worldTime, double roll, Set<String> regionsHere) {
        return matchesLocation(world, biome, worldTime, regionsHere) && roll < chance;
    }

    public boolean matchesLocation(String world, String biome, long worldTime) {
        return matchesLocation(world, biome, worldTime, Set.of());
    }

    public boolean matchesLocation(String world, String biome, long worldTime, Set<String> regionsHere) {
        if (!regions.isEmpty() && regions.stream().noneMatch(regionsHere::contains)) return false;
        if (notRegions.stream().anyMatch(regionsHere::contains)) return false;
        if (!worlds.isEmpty() && !worlds.contains(world.toLowerCase(Locale.ROOT))) return false;
        if (!biomes.isEmpty() && !biomes.contains(biomeKey(biome))) return false;
        boolean night = worldTime >= NIGHT_START && worldTime < NIGHT_END;
        if (time == Time.NIGHT && !night || time == Time.DAY && night) return false;
        return true;
    }

    private static String biomeKey(String biome) {
        String key = biome.trim().toLowerCase(Locale.ROOT);
        return key.startsWith("minecraft:") ? key.substring("minecraft:".length()) : key;
    }
}
