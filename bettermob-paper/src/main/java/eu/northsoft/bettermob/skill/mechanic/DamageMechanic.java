package eu.northsoft.bettermob.skill.mechanic;

import eu.northsoft.bettermob.skill.SkillEngine;
import org.bukkit.entity.LivingEntity;

import static eu.northsoft.bettermob.skill.Params.firstParam;
import static eu.northsoft.bettermob.skill.Params.parseFloat;

public final class DamageMechanic implements Mechanic {
    private final SkillEngine engine;

    public DamageMechanic(SkillEngine engine) {
        this.engine = engine;
    }

    @Override
    public void execute(MechanicCall call) {
        if (!(call.target().entity() instanceof LivingEntity victim)) return;
        double amount = parseFloat(firstParam(call.params(), "amount", "a"), 1f);
        engine.state().applyDamage(victim, amount, call.context().caster());
    }
}
