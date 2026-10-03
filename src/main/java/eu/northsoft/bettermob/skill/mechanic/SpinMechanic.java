package eu.northsoft.bettermob.skill.mechanic;

import eu.northsoft.bettermob.skill.SkillEngine;
import eu.northsoft.bettermob.util.Tasks;
import org.bukkit.entity.Entity;

import java.util.Map;

import static eu.northsoft.bettermob.skill.Params.parseFloat;
import static eu.northsoft.bettermob.skill.Params.parseInt;

public final class SpinMechanic implements Mechanic {
    private final SkillEngine engine;

    public SpinMechanic(SkillEngine engine) {
        this.engine = engine;
    }

    @Override
    public void execute(MechanicCall call) {
        Map<String, String> p = call.params();
        Entity entity = call.target().entity();
        if (entity == null) return;
        int duration = parseInt(p.get("duration"), 100);
        float velocity = parseFloat(p.get("velocity"), 10f);
        int[] elapsed = {0};
        Runnable[] cancel = new Runnable[1];
        cancel[0] = Tasks.runTimer(engine.plugin(), entity, 1L, 1L, () -> {
            if (++elapsed[0] > duration || !entity.isValid()) {
                cancel[0].run();
                return;
            }
            entity.setRotation(entity.getYaw() + velocity, entity.getPitch());
        });
    }
}
