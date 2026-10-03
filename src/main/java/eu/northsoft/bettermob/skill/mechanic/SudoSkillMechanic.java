package eu.northsoft.bettermob.skill.mechanic;

import eu.northsoft.bettermob.skill.SkillContext;
import eu.northsoft.bettermob.skill.SkillEngine;
import org.bukkit.entity.LivingEntity;

import static eu.northsoft.bettermob.skill.Params.firstParam;

public final class SudoSkillMechanic implements Mechanic {
    private final SkillEngine engine;

    public SudoSkillMechanic(SkillEngine engine) {
        this.engine = engine;
    }

    @Override
    public void execute(MechanicCall call) {
        if (!(call.target().entity() instanceof LivingEntity executor)) return;
        String id = firstParam(call.params(), "s", "skill", "skills");
        if (id != null) engine.runById(id.trim(), new SkillContext(executor, call.context().caster(), null));
    }
}
