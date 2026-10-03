package eu.northsoft.bettermob.skill.mechanic;

import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.util.Vector;

import java.util.Map;

import static eu.northsoft.bettermob.skill.Params.firstParam;
import static eu.northsoft.bettermob.skill.Params.parseFloat;

public final class ThrowMechanic implements Mechanic {
    @Override
    public void execute(MechanicCall call) {
        Map<String, String> p = call.params();
        LivingEntity caster = call.context().caster();
        Entity thrown = call.target().entity();
        if (thrown == null || thrown instanceof LivingEntity living && living.isDead()) return;
        Vector away = thrown.getLocation().toVector().subtract(caster.getLocation().toVector()).setY(0);
        if (away.lengthSquared() < 1e-6) away = caster.getLocation().getDirection().setY(0);
        thrown.setVelocity(away.normalize().multiply(parseFloat(firstParam(p, "velocity", "v"), 4f) / 10.0)
                .setY(parseFloat(firstParam(p, "velocityy", "vy"), 0f) / 10.0));
    }
}
