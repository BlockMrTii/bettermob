package eu.northsoft.bettermob.skill.mechanic;

import org.bukkit.Location;

import static eu.northsoft.bettermob.skill.Params.firstParam;
import static eu.northsoft.bettermob.skill.Params.parseFloat;

public final class ExplosionMechanic implements Mechanic {
    @Override
    public void execute(MechanicCall call) {
        Location at = call.target().location();
        if (at == null) return;
        float power = parseFloat(firstParam(call.params(), "yield", "y", "power"), 2f);
        boolean blockDamage = "true".equalsIgnoreCase(firstParam(call.params(), "bd", "blockdamage"));
        boolean fire = "true".equalsIgnoreCase(call.params().get("fire"));
        at.getWorld().createExplosion(at, power, fire, blockDamage, call.context().caster());
    }
}
