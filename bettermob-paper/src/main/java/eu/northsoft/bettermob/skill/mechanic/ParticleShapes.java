package eu.northsoft.bettermob.skill.mechanic;

import org.bukkit.util.Vector;

import java.util.ArrayList;
import java.util.List;

final class ParticleShapes {
    static final int MAX_POINTS = 500;

    private ParticleShapes() {}

    static int clamp(int points) {
        return Math.max(1, Math.min(MAX_POINTS, points));
    }

    static List<Vector> line(Vector from, Vector to, int points) {
        int count = Math.max(2, clamp(points));
        List<Vector> result = new ArrayList<>(count);
        Vector step = to.clone().subtract(from).multiply(1.0 / (count - 1));
        for (int i = 0; i < count; i++) result.add(from.clone().add(step.clone().multiply(i)));
        return result;
    }

    static int linePoints(Vector from, Vector to, double perBlock) {
        return clamp((int) Math.ceil(from.distance(to) * Math.max(0.1, perBlock)) + 1);
    }

    static List<Vector> sphere(double radius, int points) {
        int count = clamp(points);
        List<Vector> result = new ArrayList<>(count);
        double golden = Math.PI * (3 - Math.sqrt(5));
        for (int i = 0; i < count; i++) {
            double y = count == 1 ? 0 : 1 - 2.0 * i / (count - 1);
            double ring = Math.sqrt(Math.max(0, 1 - y * y));
            double angle = golden * i;
            result.add(new Vector(Math.cos(angle) * ring * radius, y * radius, Math.sin(angle) * ring * radius));
        }
        return result;
    }

    static List<Vector> helix(double radius, double height, double turns, int points) {
        int count = clamp(points);
        List<Vector> result = new ArrayList<>(count);
        for (int i = 0; i < count; i++) {
            double fraction = count == 1 ? 0 : (double) i / (count - 1);
            double angle = fraction * turns * 2 * Math.PI;
            result.add(new Vector(Math.cos(angle) * radius, fraction * height, Math.sin(angle) * radius));
        }
        return result;
    }
}
