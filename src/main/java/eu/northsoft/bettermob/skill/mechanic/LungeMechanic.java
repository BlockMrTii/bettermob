package eu.northsoft.bettermob.skill.mechanic;

import org.bukkit.Location;
import org.bukkit.entity.LivingEntity;
import org.bukkit.util.Vector;

import java.util.Map;

import static eu.northsoft.bettermob.skill.Params.firstParam;
import static eu.northsoft.bettermob.skill.Params.parseFloat;

public final class LungeMechanic implements Mechanic {
    @Override
    public boolean runsOnTarget() {
        return false;
    }

    @Override
    public void execute(MechanicCall call) {
        Map<String, String> p = call.params();
        LivingEntity caster = call.context().caster();
        Location to = call.target().ownedLocation();
        if (to == null) return;
        Vector toward = to.toVector().subtract(caster.getLocation().toVector()).setY(0);
        if (toward.lengthSquared() < 1e-6) return;
        caster.setVelocity(toward.normalize().multiply(parseFloat(p.get("velocity"), 1f))
                .setY(parseFloat(firstParam(p, "velocityy", "vy"), 0f)));
    }
}
