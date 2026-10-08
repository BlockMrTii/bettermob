package eu.northsoft.bettermob.skill.mechanic;

import org.bukkit.Bukkit;
import org.bukkit.Location;

import static eu.northsoft.bettermob.skill.Params.firstParam;
import static eu.northsoft.bettermob.skill.Params.parseFloat;

public final class ExplosionMechanic implements Mechanic {
    private static final float SAFE_POWER = 4f;
    private static final double BLOCKS_PER_POWER = 2.0;
    private static final double CHUNK_SIZE = 16.0;

    @Override
    public void execute(MechanicCall call) {
        Location at = call.target().location();
        if (at == null) return;
        float power = parseFloat(firstParam(call.params(), "yield", "y", "power"), 2f);
        if (!Float.isFinite(power)) power = 2f;
        while (power > SAFE_POWER && !Bukkit.isOwnedByCurrentRegion(at, (int) Math.ceil(power * BLOCKS_PER_POWER / CHUNK_SIZE))) power /= 2;
        boolean blockDamage = "true".equalsIgnoreCase(firstParam(call.params(), "bd", "blockdamage"));
        boolean fire = "true".equalsIgnoreCase(call.params().get("fire"));
        at.getWorld().createExplosion(at, power, fire, blockDamage, call.context().caster());
    }
}
