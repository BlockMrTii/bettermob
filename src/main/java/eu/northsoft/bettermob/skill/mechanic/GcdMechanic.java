package eu.northsoft.bettermob.skill.mechanic;

import eu.northsoft.bettermob.skill.SkillEngine;

import static eu.northsoft.bettermob.skill.Params.parseInt;

public final class GcdMechanic implements Mechanic {
    private final SkillEngine engine;

    public GcdMechanic(SkillEngine engine) {
        this.engine = engine;
    }

    @Override
    public boolean runsOnTarget() {
        return false;
    }

    @Override
    public void execute(MechanicCall call) {
        engine.state().setGcd(call.context().caster().getUniqueId(), parseInt(call.params().get("ticks"), 20));
    }
}
