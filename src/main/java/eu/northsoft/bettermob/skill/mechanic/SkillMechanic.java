package eu.northsoft.bettermob.skill.mechanic;

import eu.northsoft.bettermob.skill.SkillContext;
import eu.northsoft.bettermob.skill.SkillEngine;
import org.bukkit.entity.LivingEntity;

import static eu.northsoft.bettermob.skill.Params.firstParam;

public final class SkillMechanic implements Mechanic {
    private final SkillEngine engine;

    public SkillMechanic(SkillEngine engine) {
        this.engine = engine;
    }

    @Override
    public boolean runsOnTarget() {
        return false;
    }

    @Override
    public void execute(MechanicCall call) {
        String id = firstParam(call.params(), "s", "skill", "skills");
        if (id == null) return;

        boolean forTarget = !call.step().targeter().isEmpty() && !call.step().targeter().equals("self")
                && call.target().entity() instanceof LivingEntity;
        SkillContext child = forTarget ? call.context().withTrigger((LivingEntity) call.target().entity()) : call.context();
        if (id.trim().startsWith("[")) engine.runInline(id, child);
        else engine.runById(id.trim(), child);
    }
}
