package eu.northsoft.bettermob.skill.mechanic;

import eu.northsoft.bettermob.BetterMobPlugin;
import org.bukkit.Location;
import org.bukkit.Particle;

import java.util.Locale;
import java.util.Map;

import static eu.northsoft.bettermob.skill.Params.firstParam;
import static eu.northsoft.bettermob.skill.Params.parseFloat;
import static eu.northsoft.bettermob.skill.Params.parseInt;

final class ParticleSupport {
    private static final int MAX_AMOUNT = 1000;

    private ParticleSupport() {}

    static void spawn(BetterMobPlugin plugin, Location location, Particle particle, Map<String, String> p) {
        int amount = Math.max(1, Math.min(MAX_AMOUNT, parseInt(firstParam(p, "amount", "a"), 1)));
        double horizontal = parseFloat(p.get("hs"), 0f);
        double vertical = parseFloat(p.get("vs"), 0f);
        double speed = parseFloat(firstParam(p, "speed", "s"), 0f);
        try {
            location.getWorld().spawnParticle(particle, location, amount, horizontal, vertical, horizontal, speed);
        } catch (IllegalArgumentException exception) {
            plugin.messages().warn("skill.particleNeedsData", "particle", particle);
        }
    }

    static Particle parse(BetterMobPlugin plugin, String name) {
        if (name == null) return null;
        try {
            return Particle.valueOf(name.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException exception) {
            plugin.messages().warn("skill.particleUnknown", "particle", name);
            return null;
        }
    }
}
