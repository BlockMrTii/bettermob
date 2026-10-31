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
    public String customName() {
        net.minecraft.text.Text text = entity.getCustomName();
        return text != null ? text.getString() : null;
    }

    @Override
    public void customName(String name) {
        if (name == null) {
            entity.setCustomName(null);
        } else {
            entity.setCustomName(net.minecraft.text.Text.literal(name));
        }
    }

    @Override
    public boolean isCustomNameVisible() {
        return entity.isCustomNameVisible();
    }

    @Override
    public void customNameVisible(boolean visible) {
        entity.setCustomNameVisible(visible);
    }

    @Override
    public boolean isPlayer() {
        return entity instanceof net.minecraft.entity.player.PlayerEntity;
    }

    @Override
    public void sendMessage(String message) {
        if (message != null && entity instanceof net.minecraft.entity.player.PlayerEntity pe) {
            pe.sendMessage(net.minecraft.text.Text.literal(message.replace('&', '§')), false);
        }
    }

    @Override
    public void sendActionBar(String message) {
        if (message != null && entity instanceof net.minecraft.entity.player.PlayerEntity pe) {
            pe.sendMessage(net.minecraft.text.Text.literal(message.replace('&', '§')), true);
        }
    }

    @Override
    public void setFireTicks(int ticks) {
        entity.setFireTicks(ticks);
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
