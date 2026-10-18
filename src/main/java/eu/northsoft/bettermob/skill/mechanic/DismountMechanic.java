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
        if (!modelOnly) owner.eject();
        for (UUID id : engine.modelRiders(owner.getUniqueId(), null)) {
            Entity rider = Bukkit.getEntity(id);
            if (rider == null) {
                engine.modelPassengers().remove(id);
                continue;
            }
            Tasks.runOwned(engine.plugin(), rider, () -> {
                engine.betterModel().dismount(engine.mobManager().trackerFor(owner.getUniqueId()), rider);
                engine.modelEngine().dismount(engine.mobManager().modelEngineTrackerFor(owner.getUniqueId()), rider);
                if (!rider.isInsideVehicle() || rider.leaveVehicle()) engine.modelPassengers().remove(id);
            });
        }
    }
}
