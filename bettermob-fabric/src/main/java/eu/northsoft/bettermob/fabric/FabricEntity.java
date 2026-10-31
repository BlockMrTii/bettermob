package eu.northsoft.bettermob.fabric;

import eu.northsoft.bettermob.platform.PlatformEntity;
import eu.northsoft.bettermob.platform.PlatformWorld;
import eu.northsoft.bettermob.platform.Position;
import eu.northsoft.bettermob.platform.Vec3;
import net.minecraft.entity.Entity;
import net.minecraft.registry.Registries;
import net.minecraft.server.world.ServerWorld;

import java.util.UUID;

public class FabricEntity implements PlatformEntity {
    protected final Entity entity;

    public FabricEntity(Entity entity) {
        this.entity = entity;
    }

    @Override
    public UUID uuid() {
        return entity.getUuid();
    }

    @Override
    public String typeKey() {
        return Registries.ENTITY_TYPE.getId(entity.getType()).toString();
    }

    @Override
    public Position position() {
        String worldKey = entity.getEntityWorld() instanceof ServerWorld sw
                ? sw.getRegistryKey().getValue().toString()
                : "minecraft:overworld";
        return new Position(worldKey, entity.getX(), entity.getY(), entity.getZ(), entity.getYaw(), entity.getPitch());
    }

    @Override
    public PlatformWorld world() {
        return entity.getEntityWorld() instanceof ServerWorld sw ? new FabricWorld(sw) : null;
    }

    @Override
    public boolean isValid() {
        return entity.isAlive() && !entity.isRemoved();
    }

    @Override
    public void teleport(Position position) {
        entity.requestTeleport(position.x(), position.y(), position.z());
    }

    @Override
    public Vec3 velocity() {
        return FabricAdapters.vec(entity.getVelocity());
    }

    @Override
    public void velocity(Vec3 velocity) {
        entity.setVelocity(FabricAdapters.vec3d(velocity));
    }

    @Override
    public void remove() {
        entity.discard();
    }

    @Override
    public Entity handle() {
        return entity;
    }

    @Override
    public boolean equals(Object other) {
        return other instanceof FabricEntity fe && fe.entity.getUuid().equals(entity.getUuid());
    }

    @Override
    public int hashCode() {
        return entity.getUuid().hashCode();
    }
}
