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
    public void playSound(Position position, String soundKey, float volume, float pitch) {
        if (soundKey == null) return;
        org.bukkit.Location loc = PaperAdapters.location(world, position);
        world.playSound(loc, soundKey, org.bukkit.SoundCategory.HOSTILE, volume, pitch);
    }

    @Override
    public void strikeLightning(Position position, boolean damage) {
        org.bukkit.Location loc = PaperAdapters.location(world, position);
        if (damage) {
            world.strikeLightning(loc);
        } else {
            world.strikeLightningEffect(loc);
        }
    }

    @Override
    public Optional<PlatformEntity> nearestPlayer(Position center, double maxDistance) {
        org.bukkit.Location loc = PaperAdapters.location(world, center);
        double maxSq = maxDistance * maxDistance;
        return world.getNearbyPlayers(loc, maxDistance).stream()
                .filter(p -> p.getLocation().distanceSquared(loc) <= maxSq)
                .min(java.util.Comparator.comparingDouble(p -> p.getLocation().distanceSquared(loc)))
                .map(PaperAdapters::entity);
    }

    @Override
    public void dropItem(Position position, String itemType, int count) {
        if (itemType == null || count <= 0) return;
        org.bukkit.Material mat = org.bukkit.Material.matchMaterial(itemType);
        if (mat != null && mat.isItem()) {
            world.dropItemNaturally(PaperAdapters.location(world, position), new org.bukkit.inventory.ItemStack(mat, count));
        }
    }

    @Override
    public void dropExperience(Position position, int amount) {
        if (amount <= 0) return;
        world.spawn(PaperAdapters.location(world, position), org.bukkit.entity.ExperienceOrb.class).setExperience(amount);
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
