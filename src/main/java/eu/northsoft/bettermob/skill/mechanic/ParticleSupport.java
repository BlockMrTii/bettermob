package eu.northsoft.bettermob.skill.mechanic;

import eu.northsoft.bettermob.BetterMobPlugin;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.block.data.BlockData;
import org.bukkit.inventory.ItemStack;

import java.util.Locale;
import java.util.Map;

import static eu.northsoft.bettermob.skill.Params.firstParam;
import static eu.northsoft.bettermob.skill.Params.parseFloat;
import static eu.northsoft.bettermob.skill.Params.parseInt;

final class ParticleSupport {
    private static final int MAX_AMOUNT = 1000;
    private static final Map<String, String> LEGACY = Map.ofEntries(
            Map.entry("BLOCK_CRACK", "BLOCK"), Map.entry("BLOCKCRACK", "BLOCK"),
            Map.entry("BLOCK_DUST", "BLOCK"), Map.entry("BLOCKDUST", "BLOCK"),
            Map.entry("ITEM_CRACK", "ITEM"), Map.entry("ITEMCRACK", "ITEM"),
            Map.entry("REDDUST", "DUST"), Map.entry("REDSTONE", "DUST"),
            Map.entry("SPELL_MOB", "ENTITY_EFFECT"), Map.entry("SPELL_MOB_AMBIENT", "ENTITY_EFFECT"),
            Map.entry("MOB_SPELL", "ENTITY_EFFECT"), Map.entry("MOBSPELL", "ENTITY_EFFECT"));

    private ParticleSupport() {}

    static void spawn(BetterMobPlugin plugin, Location location, Particle particle, Map<String, String> p) {
        int amount = Math.max(1, Math.min(MAX_AMOUNT, parseInt(firstParam(p, "amount", "a"), 1)));
        double spread = parseFloat(p.get("spread"), 0f);
        double horizontal = parseFloat(firstParam(p, "hs", "hspread"), (float) spread);
        double vertical = parseFloat(firstParam(p, "vs", "vspread"), (float) spread);
        double speed = parseFloat(firstParam(p, "speed", "s"), 0f);
        try {
            location.getWorld().spawnParticle(particle, location, amount, horizontal, vertical, horizontal, speed, data(particle, p));
        } catch (IllegalArgumentException exception) {
            plugin.messages().warn("skill.particleNeedsData", "particle", particle);
        }
    }

    static Object data(Particle particle, Map<String, String> p) {
        Class<?> type = particle.getDataType();
        if (type == Void.class) return null;
        Color color = Color.fromRGB(ModelEffectMechanic.parseColor(firstParam(p, "color", "c"), 0xFF0000));
        float size = parseFloat(firstParam(p, "size"), 1f);
        if (type == BlockData.class) return material(p, Material.STONE).createBlockData();
        if (type == ItemStack.class) return new ItemStack(material(p, Material.STONE));
        if (type == Particle.DustOptions.class) return new Particle.DustOptions(color, size);
        if (type == Particle.DustTransition.class) {
            Color to = Color.fromRGB(ModelEffectMechanic.parseColor(firstParam(p, "tocolor", "color2"), color.asRGB()));
            return new Particle.DustTransition(color, to, size);
        }
        if (type == Color.class) return color;
        if (type == Float.class) return parseFloat(firstParam(p, "roll"), 0f);
        if (type == Integer.class) return parseInt(firstParam(p, "delay"), 0);
        return null;
    }

    private static Material material(Map<String, String> p, Material fallback) {
        String name = firstParam(p, "material", "m", "block", "b", "item");
        Material material = name == null ? null : Material.matchMaterial(name);
        return material == null ? fallback : material;
    }

    static Particle parse(BetterMobPlugin plugin, String name) {
        if (name == null) return null;
        String key = name.trim().toUpperCase(Locale.ROOT);
        try {
            return Particle.valueOf(LEGACY.getOrDefault(key, key));
        } catch (IllegalArgumentException exception) {
            plugin.messages().warn("skill.particleUnknown", "particle", name);
            return null;
        }
    }
}
