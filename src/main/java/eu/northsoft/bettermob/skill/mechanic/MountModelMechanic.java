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
        Object tracker = engine.mobManager().trackerFor(call.context().caster().getUniqueId());
        if (tracker == null) {
            engine.plugin().messages().warn("skill.mountNoTracker");
            return;
        }
        engine.betterModel().mount(tracker, call.params().getOrDefault("seat", "mount"), rider);
    }
}
