package eu.northsoft.bettermob.skill.mechanic;

import org.bukkit.util.Vector;

final class ProjectileMotion {
    private ProjectileMotion() {
    }

    static Vector reflect(Vector direction, Vector normal) {
        Vector n = normal.clone().normalize();
        return direction.clone().subtract(n.multiply(2 * direction.dot(n))).normalize();
    }

    static Vector steer(Vector direction, Vector toTarget, double maxTurnRadians) {
        if (toTarget.lengthSquared() < 1.0e-9) return direction.clone();
        Vector from = direction.clone().normalize();
        Vector to = toTarget.clone().normalize();
        double angle = Math.acos(Math.max(-1, Math.min(1, from.dot(to))));
        if (angle <= maxTurnRadians || angle < 1.0e-6) return to;
        Vector perpendicular = to.clone().subtract(from.clone().multiply(from.dot(to)));
        if (perpendicular.lengthSquared() < 1.0e-9) perpendicular = from.clone().crossProduct(Math.abs(from.getY()) < 0.9 ? new Vector(0, 1, 0) : new Vector(1, 0, 0));
        perpendicular.normalize();
        return from.multiply(Math.cos(maxTurnRadians)).add(perpendicular.multiply(Math.sin(maxTurnRadians))).normalize();
    }
}
