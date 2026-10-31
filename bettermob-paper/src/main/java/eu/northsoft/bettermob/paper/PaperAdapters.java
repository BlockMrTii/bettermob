package eu.northsoft.bettermob.paper;

import eu.northsoft.bettermob.platform.PlatformEntity;
import eu.northsoft.bettermob.platform.Position;
import eu.northsoft.bettermob.platform.Vec3;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.util.Vector;

/** Conversions between Bukkit types and the platform-neutral core types. */
public final class PaperAdapters {
    private PaperAdapters() {
    }

    public static Position position(Location location) {
        World world = location.getWorld();
        if (world == null) throw new IllegalArgumentException("location has no world");
        return new Position(world.getName(), location.getX(), location.getY(), location.getZ(), location.getYaw(), location.getPitch());
    }

    /** Returns {@code null} if the world is not loaded. */
    public static Location location(Position position) {
        World world = Bukkit.getWorld(position.world());
        return world == null ? null : location(world, position);
    }

    public static Location location(World world, Position position) {
        return new Location(world, position.x(), position.y(), position.z(), position.yaw(), position.pitch());
    }

    public static Vec3 vec(Vector vector) {
        return new Vec3(vector.getX(), vector.getY(), vector.getZ());
    }

    public static Vector vector(Vec3 vec) {
        return new Vector(vec.x(), vec.y(), vec.z());
    }

    public static PlatformEntity entity(Entity entity) {
        if (entity == null) return null;
        return entity instanceof LivingEntity living ? new PaperLivingEntity(living) : new PaperEntity(entity);
    }

    public static Entity bukkit(PlatformEntity entity) {
        if (entity == null) return null;
        if (entity.handle() instanceof Entity bukkit) return bukkit;
        throw new IllegalArgumentException("not a Paper entity: " + entity.getClass().getName());
    }
}
