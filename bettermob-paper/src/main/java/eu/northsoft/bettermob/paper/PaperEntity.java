package eu.northsoft.bettermob.paper;

import eu.northsoft.bettermob.platform.PlatformEntity;
import eu.northsoft.bettermob.platform.PlatformWorld;
import eu.northsoft.bettermob.platform.Position;
import eu.northsoft.bettermob.platform.Vec3;
import org.bukkit.entity.Entity;

import java.util.UUID;

public class PaperEntity implements PlatformEntity {
    protected final Entity entity;

    public PaperEntity(Entity entity) {
        this.entity = entity;
    }

    @Override
    public UUID uuid() {
        return entity.getUniqueId();
    }

    @Override
    public String typeKey() {
        return entity.getType().getKey().toString();
    }

    @Override
    public Position position() {
        return PaperAdapters.position(entity.getLocation());
    }

    @Override
    public PlatformWorld world() {
        return new PaperWorld(entity.getWorld());
    }

    @Override
    public boolean isValid() {
        return entity.isValid();
    }

    @Override
    public void teleport(Position position) {
        entity.teleportAsync(PaperAdapters.location(entity.getWorld(), position));
    }

    @Override
    public Vec3 velocity() {
        return PaperAdapters.vec(entity.getVelocity());
    }

    @Override
    public void velocity(Vec3 velocity) {
        entity.setVelocity(PaperAdapters.vector(velocity));
    }

    @Override
    public void remove() {
        entity.remove();
    }

    @Override
    public String customName() {
        return entity.getCustomName();
    }

    @Override
    public void customName(String name) {
        entity.setCustomName(name);
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
        return entity instanceof org.bukkit.entity.Player;
    }

    @Override
    public void sendMessage(String message) {
        if (message != null) {
            entity.sendMessage(net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer.legacySection().deserialize(message.replace('&', '§')));
        }
    }

    @Override
    public void sendActionBar(String message) {
        if (message != null && entity instanceof org.bukkit.entity.Player player) {
            player.sendActionBar(net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer.legacySection().deserialize(message.replace('&', '§')));
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
        return other instanceof PaperEntity paper && paper.entity.getUniqueId().equals(entity.getUniqueId());
    }

    @Override
    public int hashCode() {
        return entity.getUniqueId().hashCode();
    }
}
