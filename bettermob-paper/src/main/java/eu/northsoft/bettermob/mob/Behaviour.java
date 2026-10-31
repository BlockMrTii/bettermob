package eu.northsoft.bettermob.mob;

import org.bukkit.configuration.ConfigurationSection;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.function.Consumer;

public record Behaviour(List<Waypoint> patrol, boolean loop, int waitTicks, double guardRadius, double maxHomeDistance,
                        String group, double alertRadius, Leader leader) {
    public static final double DEFAULT_ALERT_RADIUS = 16;
    public static final double MAX_RANGE = 128;

    public record Waypoint(String world, double x, double y, double z) {}

    public enum LeaderLoss { FIND, STAY, HOME }

    public record Leader(String mob, double distance, double range, LeaderLoss onLoss) {}

    public boolean hasGroup() {
        return group != null && alertRadius > 0;
    }

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
        return hasGuard() || hasHomeLimit() || (leader != null && leader.onLoss() == LeaderLoss.HOME);
    }

    public static Behaviour parse(ConfigurationSection mob, Consumer<String> invalid) {
        List<Waypoint> points = new ArrayList<>();
        boolean loop = true;
        int waitTicks = 0;
        Object patrol = mob.get("Patrol");
        List<String> lines = List.of();
        if (patrol instanceof ConfigurationSection section) {
            lines = section.getStringList("Points");
            if (section.contains("Loop")) loop = !"false".equalsIgnoreCase(String.valueOf(section.get("Loop")).trim());
            if (section.contains("Points") && !section.isList("Points")) invalid.accept("Patrol: Points " + section.get("Points"));
            waitTicks = (int) Math.round(Math.max(0, section.getDouble("Wait", 0)) * 20);
        } else if (mob.isList("Patrol")) {
            lines = mob.getStringList("Patrol");
        } else if (patrol != null) {
            invalid.accept("Patrol: " + patrol);
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
        String group = mob.get("Group") instanceof String name && !name.isBlank() ? name.trim().toLowerCase(Locale.ROOT) : null;
        double alert = mob.get("AlertRadius") instanceof Number number ? Math.max(0, Math.min(MAX_RANGE, number.doubleValue())) : DEFAULT_ALERT_RADIUS;
        Leader leader = parseLeader(mob, invalid);
        if (points.isEmpty() && radius <= 0 && maxHome <= 0 && group == null && leader == null) return null;
        return new Behaviour(List.copyOf(points), loop, waitTicks, Math.max(0, radius), maxHome, group, alert, leader);
    }

    static Leader parseLeader(ConfigurationSection mob, Consumer<String> invalid) {
        Object value = mob.get("Leader");
        if (value == null) return null;
        String id;
        double distance = 4;
        double range = 32;
        LeaderLoss loss = LeaderLoss.FIND;
        if (value instanceof ConfigurationSection section) {
            id = section.getString("Mob");
            if (section.get("Distance") instanceof Number number) distance = number.doubleValue();
            if (section.get("Range") instanceof Number number) range = number.doubleValue();
            if (section.contains("OnLeaderDeath")) {
                try {
                    loss = LeaderLoss.valueOf(String.valueOf(section.get("OnLeaderDeath")).trim().toUpperCase(Locale.ROOT));
                } catch (IllegalArgumentException exception) {
                    invalid.accept("Leader: OnLeaderDeath " + section.get("OnLeaderDeath"));
                }
            }
        } else {
            id = value instanceof String text ? text : null;
        }
        if (id == null || id.isBlank() || !Double.isFinite(distance) || !Double.isFinite(range)) {
            invalid.accept("Leader: " + value);
            return null;
        }
        return new Leader(id.trim().toLowerCase(Locale.ROOT), Math.max(1, Math.min(MAX_RANGE, distance)), Math.max(1, Math.min(MAX_RANGE, range)), loss);
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
