package eu.northsoft.bettermob.skill.mechanic;

import org.bukkit.Location;
import org.bukkit.entity.LivingEntity;

public final class LookMechanic implements Mechanic {
    @Override
    public boolean runsOnTarget() {
        return false;
    }

    @Override
    public void execute(MechanicCall call) {
        LivingEntity caster = call.context().caster();
        Location to = call.target().ownedLocation();
        if (to == null) return;
        Location from = caster.getLocation();
        Location facing = from.clone();
        facing.setDirection(to.toVector().subtract(from.toVector()));
        caster.setRotation(facing.getYaw(), facing.getPitch());
    }
}
