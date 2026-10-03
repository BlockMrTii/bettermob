package eu.northsoft.bettermob.skill;

import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.entity.Entity;

public record Target(Entity entity, Block block, Location rawLocation) {
    public static Target ofEntity(Entity entity) {
        return new Target(entity, null, null);
    }

    public static Target ofBlock(Block block) {
        return new Target(null, block, null);
    }

    public static Target ofLocation(Location location) {
        return new Target(null, null, location);
    }

    public Location location() {
        if (entity != null) return entity.getLocation();
        if (block != null) return block.getLocation().add(0.5, 0.5, 0.5);
        return rawLocation;
    }
}
