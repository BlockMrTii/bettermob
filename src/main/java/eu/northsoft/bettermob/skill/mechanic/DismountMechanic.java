package eu.northsoft.bettermob.skill.mechanic;

import eu.northsoft.bettermob.skill.SkillEngine;
import eu.northsoft.bettermob.util.Tasks;
import org.bukkit.Bukkit;
import org.bukkit.entity.Entity;

import java.util.UUID;

public final class DismountMechanic implements Mechanic {
    private final SkillEngine engine;
    private final boolean modelOnly;

    public DismountMechanic(SkillEngine engine, boolean modelOnly) {
        this.engine = engine;
        this.modelOnly = modelOnly;
    }

    @Override
    public void execute(MechanicCall call) {
        Entity owner = call.target().entity();
        if (owner == null) return;
        if (!modelOnly) {
            owner.eject();
            engine.modelPassengers().clear(owner.getUniqueId());
            return;
        }
        for (UUID id : engine.modelPassengers().clear(owner.getUniqueId())) {
            Entity rider = Bukkit.getEntity(id);
            if (rider != null) Tasks.runOwned(engine.plugin(), rider, rider::leaveVehicle);
        }
    }
}
