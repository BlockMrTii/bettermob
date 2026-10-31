package eu.northsoft.bettermob.mob;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.NamespacedKey;
import org.bukkit.World;
import org.bukkit.entity.Entity;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.Plugin;

import java.util.UUID;
import java.util.function.Function;

final class MobHome {
    private final NamespacedKey key;

    MobHome(Plugin plugin) {
        this.key = new NamespacedKey(plugin, "home");
    }

    Location of(Entity entity) {
        String stored = entity.getPersistentDataContainer().get(key, PersistentDataType.STRING);
        return parse(stored, entity.getWorld(), Bukkit::getWorld);
    }

    void storeIfMissing(Entity entity) {
        if (of(entity) != null) return;
        entity.getPersistentDataContainer().set(key, PersistentDataType.STRING, format(entity.getLocation()));
    }

    static String format(Location at) {
        return at.getWorld().getUID() + ";" + at.getX() + ";" + at.getY() + ";" + at.getZ();
    }

    static Location parse(String stored, World current, Function<UUID, World> worlds) {
        if (stored == null) return null;
        String[] parts = stored.split(";");
        if (parts.length != 3 && parts.length != 4) return null;
        try {
            int offset = parts.length - 3;
            World world = offset == 0 ? current : worlds.apply(UUID.fromString(parts[0]));
            if (world == null) return null;
            return new Location(world, Double.parseDouble(parts[offset]), Double.parseDouble(parts[offset + 1]), Double.parseDouble(parts[offset + 2]));
        } catch (IllegalArgumentException exception) {
            return null;
        }
    }
}
