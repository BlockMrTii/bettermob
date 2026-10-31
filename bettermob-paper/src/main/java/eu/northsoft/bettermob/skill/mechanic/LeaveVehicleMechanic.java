package eu.northsoft.bettermob.skill.mechanic;

import org.bukkit.entity.Entity;

public final class LeaveVehicleMechanic implements Mechanic {
    @Override
    public void execute(MechanicCall call) {
        Entity entity = call.target().entity();
        if (entity != null && entity.isInsideVehicle()) entity.leaveVehicle();
    }
}
