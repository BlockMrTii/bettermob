package eu.northsoft.bettermob.skill.mechanic;

import eu.northsoft.bettermob.skill.SkillEngine;
import org.bukkit.entity.LivingEntity;

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
        if (!(call.target().entity() instanceof LivingEntity rider)) return;
        java.util.UUID id = call.context().caster().getUniqueId();
        String seat = call.params().getOrDefault("seat", "mount");
        Object tracker = engine.mobManager().trackerFor(id);
        if (tracker != null) {
            engine.betterModel().mount(tracker, seat, rider);
            return;
        }
        Object engineTracker = engine.mobManager().modelEngineTrackerFor(id);
        if (engineTracker == null) {
            engine.plugin().messages().warn("skill.mountNoTracker");
            return;
        }
        engine.modelEngine().mount(engineTracker, seat, rider, call.params().get("controller"));
    }
}
