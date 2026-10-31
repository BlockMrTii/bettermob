package eu.northsoft.bettermob.fabric;

import eu.northsoft.bettermob.platform.PlatformEntity;
import eu.northsoft.bettermob.platform.Position;
import eu.northsoft.bettermob.platform.Vec3;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.util.math.Vec3d;

public final class FabricAdapters {
    private FabricAdapters() {}

    public static Vec3 vec(Vec3d vec) {
        return new Vec3(vec.getX(), vec.getY(), vec.getZ());
    }

    public static Vec3d vec3d(Vec3 vec) {
        return new Vec3d(vec.x(), vec.y(), vec.z());
    }

    public static PlatformEntity entity(Entity entity) {
        if (entity == null) return null;
        return entity instanceof LivingEntity living ? new FabricLivingEntity(living) : new FabricEntity(entity);
    }

    public static Entity minecraft(PlatformEntity entity) {
        if (entity == null) return null;
        if (entity.handle() instanceof Entity mc) return mc;
        throw new IllegalArgumentException("Not a Fabric entity: " + entity.getClass().getName());
    }
}
