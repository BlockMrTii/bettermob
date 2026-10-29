package eu.northsoft.bettermob.skill.mechanic;

import eu.northsoft.bettermob.skill.SkillEngine;
import eu.northsoft.bettermob.util.RegionEntities;
import eu.northsoft.bettermob.util.Tasks;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.data.BlockData;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import static eu.northsoft.bettermob.skill.Params.firstParam;
import static eu.northsoft.bettermob.skill.Params.parseInt;
import static eu.northsoft.bettermob.skill.Params.stripQuotes;

public final class BlockMaskMechanic implements Mechanic {
    private static final int MAX_RADIUS = 8;
    private static final int MAX_TICKS = 20 * 60;
    private static final double VIEW_DISTANCE = 64;

    private final SkillEngine engine;

    public BlockMaskMechanic(SkillEngine engine) {
        this.engine = engine;
    }

    static boolean inShape(int dx, int dy, int dz, int radius, boolean sphere) {
        return !sphere || dx * dx + dy * dy + dz * dz <= radius * radius;
    }

    @Override
    public void execute(MechanicCall call) {
        Location at = call.target().location();
        if (at == null || at.getWorld() == null) return;
        String materialName = firstParam(call.params(), "material", "m");
        Material material = materialName == null ? null : Material.matchMaterial(stripQuotes(materialName).toUpperCase(Locale.ROOT));
        if (material == null || !material.isBlock()) {
            engine.plugin().messages().warn("skill.blockMaskUnknown", "material", materialName);
            return;
        }
        int radius = Math.max(0, Math.min(MAX_RADIUS, parseInt(firstParam(call.params(), "radius", "r"), 0)));
        int ticks = Math.max(1, Math.min(MAX_TICKS, parseInt(firstParam(call.params(), "duration", "d"), 20)));
        boolean onlyAir = "true".equalsIgnoreCase(firstParam(call.params(), "onlyair", "oa"));
        boolean noAir = "true".equalsIgnoreCase(firstParam(call.params(), "noair", "na"));
        boolean sphere = "sphere".equalsIgnoreCase(firstParam(call.params(), "shape", "s"));
        World world = at.getWorld();
        BlockData mask = material.createBlockData();
        Map<Location, BlockData> masked = new HashMap<>();
        Map<Location, BlockData> original = new HashMap<>();
        for (int dx = -radius; dx <= radius; dx++) {
            for (int dy = -radius; dy <= radius; dy++) {
                for (int dz = -radius; dz <= radius; dz++) {
                    if (!inShape(dx, dy, dz, radius, sphere)) continue;
                    Block block = world.getBlockAt(at.getBlockX() + dx, at.getBlockY() + dy, at.getBlockZ() + dz);
                    boolean air = block.getType().isAir();
                    if (onlyAir && !air || noAir && air) continue;
                    masked.put(block.getLocation(), mask);
                    original.put(block.getLocation(), block.getBlockData());
                }
            }
        }
        if (masked.isEmpty()) return;
        for (Entity entity : RegionEntities.near(at, VIEW_DISTANCE)) {
            if (!(entity instanceof Player player)) continue;
            player.sendMultiBlockChange(masked);
            Tasks.runLater(engine.plugin(), player, ticks, () -> player.sendMultiBlockChange(original));
        }
    }
}
