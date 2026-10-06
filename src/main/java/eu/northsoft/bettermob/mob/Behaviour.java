package eu.northsoft.bettermob.mob;

import org.bukkit.configuration.ConfigurationSection;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.function.Consumer;

public record Behaviour(List<Waypoint> patrol, boolean loop, int waitTicks, double guardRadius, double maxHomeDistance) {
    public record Waypoint(String world, double x, double y, double z) {}

    public boolean hasPatrol() {
        return !patrol.isEmpty();
    }

    public boolean hasGuard() {
        return guardRadius > 0;
    }

    public boolean hasHomeLimit() {
        return maxHomeDistance > 0;
    }

    public boolean needsHome() {
        return hasGuard() || hasHomeLimit();
    }

    public static Behaviour parse(ConfigurationSection mob, Consumer<String> invalid) {
        List<Waypoint> points = new ArrayList<>();
        boolean loop = true;
        int waitTicks = 0;
        Object patrol = mob.get("Patrol");
        List<String> lines = List.of();
        if (patrol instanceof ConfigurationSection section) {
            lines = section.getStringList("Points");
            if (section.contains("Loop")) loop = section.getBoolean("Loop", true);
            waitTicks = (int) Math.round(Math.max(0, section.getDouble("Wait", 0)) * 20);
        } else if (mob.isList("Patrol")) {
            lines = mob.getStringList("Patrol");
        }
        for (String line : lines) {
            Waypoint point = parsePoint(line);
            if (point == null) invalid.accept("Patrol: " + line);
            else points.add(point);
        }
        double radius = 0;
        if (mob.isConfigurationSection("Guard")) radius = mob.getConfigurationSection("Guard").getDouble("Radius", 0);
        else if (mob.get("Guard") instanceof Number number) radius = number.doubleValue();
        double maxHome = mob.get("MaxHomeDistance") instanceof Number number ? Math.max(0, number.doubleValue()) : 0;
        if (points.isEmpty() && radius <= 0 && maxHome <= 0) return null;
        return new Behaviour(List.copyOf(points), loop, waitTicks, Math.max(0, radius), maxHome);
    }

    public static Waypoint parsePoint(String line) {
        if (line == null) return null;
        String[] parts = line.trim().split("\\s+");
        try {
            if (parts.length == 3) return new Waypoint(null, finite(parts[0]), finite(parts[1]), finite(parts[2]));
            if (parts.length == 4) return new Waypoint(parts[0].toLowerCase(Locale.ROOT), finite(parts[1]), finite(parts[2]), finite(parts[3]));
        } catch (NumberFormatException exception) {
            return null;
        }
        return null;
    }

    private static double finite(String value) {
        double number = Double.parseDouble(value);
        if (!Double.isFinite(number)) throw new NumberFormatException(value);
        return number;
    }
}
