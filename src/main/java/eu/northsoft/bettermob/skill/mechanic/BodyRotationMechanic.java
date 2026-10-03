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
    public void execute(MechanicCall call) {
        Map<String, String> p = call.params();
        UUID id = call.context().caster().getUniqueId();
        if (engine.betterModel().bodyRotation(engine.mobManager().trackerFor(id), p)) return;
        AtomicBoolean applied = new AtomicBoolean();
        for (int attempt = 1; attempt <= 5; attempt++) {
            Tasks.runLater(engine.plugin(), call.context().caster(), attempt * 2L, () -> {
                Object tracker = engine.mobManager().trackerFor(id);
                if (tracker != null && !applied.get() && engine.betterModel().bodyRotation(tracker, p)) applied.set(true);
            });
        }
    }
}
