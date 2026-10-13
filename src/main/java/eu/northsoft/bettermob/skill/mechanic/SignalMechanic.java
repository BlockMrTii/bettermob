package eu.northsoft.bettermob.skill.mechanic;

import eu.northsoft.bettermob.skill.SkillEngine;
import org.bukkit.entity.LivingEntity;

import static eu.northsoft.bettermob.skill.Params.firstParam;

public final class SignalMechanic implements Mechanic {
    private final SkillEngine engine;

    public SignalMechanic(SkillEngine engine) {
        this.engine = engine;
    }

    @Override
    public void execute(MechanicCall call) {
        if (!(call.target().entity() instanceof LivingEntity target)) return;
        String name = firstParam(call.params(), "s", "signal", "name");
        if (name == null || name.isBlank()) return;
        boolean fired = engine.mobManager().signal(target, name.trim(), call.context().caster());
        if (!fired && engine.debug().verbose()) engine.debug().verbose("signal " + name.trim() + " reached no ~onSignal skill on " + target.getName(), engine.subject(call.context().caster()));
    }
}
