package eu.northsoft.bettermob.skill.mechanic;

import eu.northsoft.bettermob.skill.SkillEngine;

public final class ModelEngineMechanic implements Mechanic {
    private final SkillEngine engine;

    public ModelEngineMechanic(SkillEngine engine) {
        this.engine = engine;
    }

    @Override
    public void execute(MechanicCall call) {
        String modelId = call.params().get("mid");
        if (modelId == null || call.target().entity() == null) return;
        Object tracker = engine.modelEngine().attach(call.target().entity(), modelId);
        engine.mobManager().replaceModelEngineTracker(call.target().entity(), tracker);
    }
}
