package eu.northsoft.bettermob.skill;

import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.entity.Entity;

record Target(Entity entity, Block block, Location rawLocation) {
    static Target ofEntity(Entity entity) {
        return new Target(entity, null, null);
    }

    static Target ofBlock(Block block) {
        return new Target(null, block, null);
    }

    static Target ofLocation(Location location) {
        return new Target(null, null, location);
    }

    Location location() {
        if (entity != null) return entity.getLocation();
        if (block != null) return block.getLocation().add(0.5, 0.5, 0.5);
        return rawLocation;
    }
}
