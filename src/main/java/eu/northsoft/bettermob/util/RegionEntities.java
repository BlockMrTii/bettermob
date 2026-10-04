package eu.northsoft.bettermob.util;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Entity;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public final class RegionEntities {
    private static final int CHUNK_SHIFT = 4;

    private RegionEntities() {
    }

    public static Collection<Entity> near(Location center, double radius) {
        World world = center.getWorld();
        if (!Tasks.FOLIA) return world.getNearbyEntities(center, radius, radius, radius);
        List<Entity> found = new ArrayList<>();
        for (int x = chunkOf(center.getX() - radius); x <= chunkOf(center.getX() + radius); x++) {
            for (int z = chunkOf(center.getZ() - radius); z <= chunkOf(center.getZ() + radius); z++) {
                if (!Bukkit.isOwnedByCurrentRegion(world, x, z) || !world.isChunkLoaded(x, z)) continue;
                for (Entity entity : world.getChunkAt(x, z, false).getEntities()) {
                    if (withinBox(entity.getLocation(), center, radius)) found.add(entity);
                }
            }
        }
        return found;
    }

    static int chunkOf(double coordinate) {
        return (int) Math.floor(coordinate) >> CHUNK_SHIFT;
    }

    static boolean withinBox(Location at, Location center, double radius) {
        return Math.abs(at.getX() - center.getX()) <= radius
                && Math.abs(at.getY() - center.getY()) <= radius
                && Math.abs(at.getZ() - center.getZ()) <= radius;
    }
}
