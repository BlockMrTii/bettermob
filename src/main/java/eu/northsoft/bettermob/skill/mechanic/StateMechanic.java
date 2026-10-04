package eu.northsoft.bettermob.skill.mechanic;

import eu.northsoft.bettermob.skill.SkillEngine;

import static eu.northsoft.bettermob.skill.Params.firstParam;

public final class StateMechanic implements Mechanic {
    private final SkillEngine engine;

    public StateMechanic(SkillEngine engine) {
        this.engine = engine;
    }

    @Override
    public boolean runsOnTarget() {
        return false;
    }

    @Override
    public void execute(MechanicCall call) {
        String stateName = firstParam(call.params(), "state", "s");
        if (stateName == null) return;
        engine.betterModel().play(engine.mobManager().trackerFor(call.context().caster().getUniqueId()), stateName);
    }
}
