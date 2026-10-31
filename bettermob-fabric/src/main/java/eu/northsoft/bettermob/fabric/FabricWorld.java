package eu.northsoft.bettermob.fabric;

import eu.northsoft.bettermob.platform.PlatformEntity;
import eu.northsoft.bettermob.platform.PlatformWorld;
import eu.northsoft.bettermob.platform.Position;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.SpawnReason;
import net.minecraft.registry.Registries;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;

import java.util.Collection;
import java.util.Optional;
import java.util.UUID;

public final class FabricWorld implements PlatformWorld {
    private final ServerWorld world;

    public FabricWorld(ServerWorld world) {
        this.world = world;
    }

    @Override
    public String key() {
        return world.getRegistryKey().getValue().toString();
    }

    @Override
    public long time() {
        return world.getTimeOfDay();
    }

    @Override
    public Optional<PlatformEntity> spawn(String typeKey, Position position) {
        if (typeKey == null) return Optional.empty();
        String normalized = typeKey.toLowerCase(java.util.Locale.ROOT);
        if (!normalized.contains(":")) normalized = "minecraft:" + normalized;
        Identifier id = Identifier.tryParse(normalized);
        if (id == null) return Optional.empty();
        EntityType<?> type = Registries.ENTITY_TYPE.get(id);
        if (type == null) return Optional.empty();

        BlockPos pos = new BlockPos(position.blockX(), position.blockY(), position.blockZ());
        net.minecraft.entity.Entity spawned = type.spawn(world, pos, SpawnReason.COMMAND);
        if (spawned != null) {
            spawned.refreshPositionAndAngles(position.x(), position.y(), position.z(), position.yaw(), position.pitch());
            return Optional.of(FabricAdapters.entity(spawned));
        }
        return Optional.empty();
    }

    @Override
    public Optional<PlatformEntity> entity(UUID uuid) {
        return Optional.ofNullable(world.getEntity(uuid)).map(FabricAdapters::entity);
    }

    @Override
    public Collection<PlatformEntity> nearbyEntities(Position center, double radius) {
        Box box = new Box(center.x() - radius, center.y() - radius, center.z() - radius,
                          center.x() + radius, center.y() + radius, center.z() + radius);
        return world.getOtherEntities(null, box).stream()
                .map(FabricAdapters::entity)
                .toList();
    }

    @Override
    public String blockKey(int x, int y, int z) {
        return Registries.BLOCK.getId(world.getBlockState(new BlockPos(x, y, z)).getBlock()).toString();
    }

    @Override
    public ServerWorld handle() {
        return world;
    }

    @Override
    public boolean equals(Object other) {
        return other instanceof FabricWorld fw && fw.world.getRegistryKey().equals(world.getRegistryKey());
    }

    @Override
    public int hashCode() {
        return world.getRegistryKey().hashCode();
    }
}
