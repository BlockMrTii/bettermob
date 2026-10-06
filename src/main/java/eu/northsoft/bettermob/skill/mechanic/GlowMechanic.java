package eu.northsoft.bettermob.skill.mechanic;

import eu.northsoft.bettermob.skill.SkillEngine;
import eu.northsoft.bettermob.util.Tasks;
import org.bukkit.entity.Entity;

import static eu.northsoft.bettermob.skill.Params.firstParam;
import static eu.northsoft.bettermob.skill.Params.parseInt;

public final class GlowMechanic implements Mechanic {
    private final SkillEngine engine;

    public GlowMechanic(SkillEngine engine) {
        this.engine = engine;
    }

    @Override
    public void execute(MechanicCall call) {
        Entity entity = call.target().entity();
        if (entity == null) return;
        boolean before = entity.isGlowing();
        entity.setGlowing(true);
        int ticks = Math.max(1, parseInt(firstParam(call.params(), "d", "duration", "t"), 40));
        Tasks.runLater(engine.plugin(), entity, ticks, () -> entity.setGlowing(before));
    }
}
