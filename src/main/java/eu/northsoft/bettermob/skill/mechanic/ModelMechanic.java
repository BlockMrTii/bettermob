package eu.northsoft.bettermob.skill.mechanic;

import eu.northsoft.bettermob.skill.SkillEngine;

public final class ModelMechanic implements Mechanic {
    private final SkillEngine engine;

    public ModelMechanic(SkillEngine engine) {
        this.engine = engine;
    }

    @Override
    public void execute(MechanicCall call) {
        String modelId = call.params().get("mid");
        if (modelId == null || call.target().entity() == null) return;
        Object tracker = engine.betterModel().attach(call.target().entity(), modelId);
        engine.mobManager().replaceTracker(call.target().entity(), tracker);
    }
}
