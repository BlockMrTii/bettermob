package eu.northsoft.bettermob.skill.mechanic;

import org.bukkit.Location;

public final class LightningMechanic implements Mechanic {
    @Override
    public void execute(MechanicCall call) {
        Location at = call.target().location();
        if (at == null) return;
        if ("true".equalsIgnoreCase(call.params().get("damage")) && !call.step().name().equals("fakelightning")) at.getWorld().strikeLightning(at);
        else at.getWorld().strikeLightningEffect(at);
    }
}
