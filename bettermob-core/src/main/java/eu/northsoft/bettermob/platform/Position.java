package eu.northsoft.bettermob.platform;

import java.util.Objects;

/**
 * A point in a world, with rotation. {@code world} is the platform-neutral world key
 * (e.g. {@code minecraft:overworld} or the Bukkit world name).
 */
public record Position(String world, double x, double y, double z, float yaw, float pitch) {
    public Position {
        Objects.requireNonNull(world, "world");
    }

    public Position(String world, double x, double y, double z) {
        this(world, x, y, z, 0f, 0f);
    }

    public Vec3 vector() {
        return new Vec3(x, y, z);
    }

    public Position add(double dx, double dy, double dz) {
        return new Position(world, x + dx, y + dy, z + dz, yaw, pitch);
    }

    public Position withRotation(float newYaw, float newPitch) {
        return new Position(world, x, y, z, newYaw, newPitch);
    }

    public int blockX() {
        return (int) Math.floor(x);
    }

    public int blockY() {
        return (int) Math.floor(y);
    }

    public int blockZ() {
        return (int) Math.floor(z);
    }

    /** Squared distance, or {@link Double#POSITIVE_INFINITY} if the worlds differ. */
    public double distanceSquared(Position other) {
        if (!world.equals(other.world)) return Double.POSITIVE_INFINITY;
        double dx = x - other.x, dy = y - other.y, dz = z - other.z;
        return dx * dx + dy * dy + dz * dz;
    }
}
