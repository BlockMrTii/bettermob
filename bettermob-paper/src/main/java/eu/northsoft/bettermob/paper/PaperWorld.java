package eu.northsoft.bettermob.paper;

import eu.northsoft.bettermob.platform.PlatformEntity;
import eu.northsoft.bettermob.platform.PlatformWorld;
import eu.northsoft.bettermob.platform.Position;
import org.bukkit.NamespacedKey;
import org.bukkit.Registry;
import org.bukkit.World;
import org.bukkit.entity.EntityType;

import java.util.Collection;
import java.util.Optional;
import java.util.UUID;

public final class PaperWorld implements PlatformWorld {
    private final World world;

    public PaperWorld(World world) {
        this.world = world;
    }

    @Override
    public String key() {
        return world.getName();
    }

    @Override
    public long time() {
        return world.getTime();
    }

    @Override
    public Optional<PlatformEntity> spawn(String typeKey, Position position) {
        NamespacedKey key = NamespacedKey.fromString(typeKey);
        EntityType type = key == null ? null : Registry.ENTITY_TYPE.get(key);
        if (type == null || !type.isSpawnable()) return Optional.empty();
        return Optional.of(PaperAdapters.entity(world.spawnEntity(PaperAdapters.location(world, position), type)));
    }

    @Override
    public Optional<PlatformEntity> entity(UUID uuid) {
        var entity = world.getEntity(uuid);
        return Optional.ofNullable(PaperAdapters.entity(entity));
    }

    @Override
    public Collection<PlatformEntity> nearbyEntities(Position center, double radius) {
        return world.getNearbyEntities(PaperAdapters.location(world, center), radius, radius, radius).stream()
                .map(PaperAdapters::entity)
                .toList();
    }

    @Override
    public String blockKey(int x, int y, int z) {
        return world.getBlockAt(x, y, z).getType().getKey().toString();
    }

    @Override
    public World handle() {
        return world;
    }

    @Override
    public boolean equals(Object other) {
        return other instanceof PaperWorld paper && paper.world.getUID().equals(world.getUID());
    }

    @Override
    public int hashCode() {
        return world.getUID().hashCode();
    }
}
