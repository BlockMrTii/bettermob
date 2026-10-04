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
        Location from = caster.getLocation();
        Location facing = from.clone();
        facing.setDirection(call.target().location().toVector().subtract(from.toVector()));
        caster.setRotation(facing.getYaw(), facing.getPitch());
    }
}
