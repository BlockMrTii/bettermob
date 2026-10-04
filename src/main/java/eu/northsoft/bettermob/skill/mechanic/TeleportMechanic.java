package eu.northsoft.bettermob.skill.mechanic;

import org.bukkit.Location;
import org.bukkit.entity.LivingEntity;

public final class TeleportMechanic implements Mechanic {
    @Override
    public void execute(MechanicCall call) {
        LivingEntity caster = call.context().caster();
        if (call.target().entity() == caster) return;
        Location destination = call.target().location();
        if (destination == null) return;
        destination = destination.clone();
        destination.setYaw(caster.getLocation().getYaw());
        destination.setPitch(caster.getLocation().getPitch());
        caster.teleportAsync(destination);
    }
}
