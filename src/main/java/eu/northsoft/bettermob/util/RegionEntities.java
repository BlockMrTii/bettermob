package eu.northsoft.bettermob.util;

import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Entity;
import org.bukkit.util.BoundingBox;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public final class RegionEntities {
    private static final int CHUNK_SHIFT = 4;
    private static final double MAX_RADIUS = 128;
    private static final double ENTITY_MARGIN = 2;

    private RegionEntities() {
    }

    public static Collection<Entity> near(Location center, double radius) {
        if (!Double.isFinite(radius) || radius < 0) return List.of();
        World world = center.getWorld();
        double scanned = Math.min(radius, MAX_RADIUS);
        if (!Tasks.FOLIA) return world.getNearbyEntities(center, scanned, scanned, scanned);
        BoundingBox search = BoundingBox.of(center, scanned, scanned, scanned);
        double reach = scanned + ENTITY_MARGIN;
        List<Entity> found = new ArrayList<>();
        for (int x = chunkOf(center.getX() - reach); x <= chunkOf(center.getX() + reach); x++) {
            for (int z = chunkOf(center.getZ() - reach); z <= chunkOf(center.getZ() + reach); z++) {
                if (!Bukkit.isOwnedByCurrentRegion(world, x, z) || !world.isChunkLoaded(x, z)) continue;
                Chunk chunk = world.getChunkAt(x, z, false);
                if (chunk == null || !chunk.isEntitiesLoaded()) continue;
                for (Entity entity : chunk.getEntities()) {
                    if (entity.getBoundingBox().overlaps(search)) found.add(entity);
                }
            }
        }
        return found;
    }

    static int chunkOf(double coordinate) {
        return (int) Math.floor(coordinate) >> CHUNK_SHIFT;
    }

}
