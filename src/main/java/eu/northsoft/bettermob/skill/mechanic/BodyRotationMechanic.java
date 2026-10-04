package eu.northsoft.bettermob.skill.mechanic;

import eu.northsoft.bettermob.skill.SkillEngine;
import eu.northsoft.bettermob.util.Tasks;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;

public final class BodyRotationMechanic implements Mechanic {
    private final SkillEngine engine;

    public BodyRotationMechanic(SkillEngine engine) {
        this.engine = engine;
    }

    @Override
    public boolean runsOnTarget() {
        return false;
    }

    @Override
    public void execute(MechanicCall call) {
        Map<String, String> p = call.params();
        UUID id = call.context().caster().getUniqueId();
        if (apply(id, p)) return;
        AtomicBoolean applied = new AtomicBoolean();
        for (int attempt = 1; attempt <= 5; attempt++) {
            Tasks.runLater(engine.plugin(), call.context().caster(), attempt * 2L, () -> {
                if (!applied.get() && apply(id, p)) applied.set(true);
            });
        }
    }

    private boolean apply(UUID id, Map<String, String> params) {
        Object tracker = engine.mobManager().trackerFor(id);
        if (tracker != null) return engine.betterModel().bodyRotation(tracker, params);
        return engine.modelEngine().bodyRotation(engine.mobManager().modelEngineTrackerFor(id), params);
    }
}
