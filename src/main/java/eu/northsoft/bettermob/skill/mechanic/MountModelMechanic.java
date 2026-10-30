package eu.northsoft.bettermob.skill.mechanic;

import eu.northsoft.bettermob.skill.Params;
import eu.northsoft.bettermob.skill.SkillEngine;
import org.bukkit.Bukkit;
import org.bukkit.entity.LivingEntity;

import java.util.UUID;

public final class MountModelMechanic implements Mechanic {
    private final SkillEngine engine;

    public MountModelMechanic(SkillEngine engine) {
        this.engine = engine;
    }

    @Override
    public boolean runsOnTarget() {
        return false;
    }

    @Override
    public void execute(MechanicCall call) {
        if (!(call.target().entity() instanceof LivingEntity rider) || !Bukkit.isOwnedByCurrentRegion(rider)) return;
        UUID id = call.context().caster().getUniqueId();
        String seat = Params.firstParam(call.params(), "seat", "p");
        if (seat == null) seat = "mount";
        Object tracker = engine.mobManager().trackerFor(id);
        if (tracker != null) {
            if (engine.betterModel().mount(tracker, seat, rider)) engine.modelPassengers().add(id, seat, rider.getUniqueId());
            return;
        }
        Object engineTracker = engine.mobManager().modelEngineTrackerFor(id);
        if (engineTracker == null) {
            engine.plugin().messages().warn("skill.mountNoTracker");
            return;
        }
        if (engine.modelEngine().mount(engineTracker, seat, rider, call.params().get("controller"))) engine.modelPassengers().add(id, seat, rider.getUniqueId());
    }
}
