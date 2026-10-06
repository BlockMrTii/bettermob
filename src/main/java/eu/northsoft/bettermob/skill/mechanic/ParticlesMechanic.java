package eu.northsoft.bettermob.skill.mechanic;

import eu.northsoft.bettermob.skill.SkillEngine;
import eu.northsoft.bettermob.skill.SkillStep;
import eu.northsoft.bettermob.skill.Target;
import eu.northsoft.bettermob.util.Tasks;
import org.bukkit.Location;
import org.bukkit.Particle;

import java.util.Map;

import static eu.northsoft.bettermob.skill.Params.firstParam;
import static eu.northsoft.bettermob.skill.Params.parseFloat;
import static eu.northsoft.bettermob.skill.Params.parseInt;

public final class ParticlesMechanic implements Mechanic {
    private final SkillEngine engine;

    public ParticlesMechanic(SkillEngine engine) {
        this.engine = engine;
    }

    @Override
    public void execute(MechanicCall call) {
        Map<String, String> p = call.params();
        SkillStep.Mechanic step = call.step();
        Particle particle = ParticleSupport.parse(engine.plugin(), firstParam(p, "p", "particle"));
        if (particle == null) return;
        double yOffset = parseFloat(firstParam(p, "y", "yoffset"), 0f);
        ParticleSupport.spawn(engine.plugin(), call.target().location().clone().add(0, yOffset, 0), particle, p);

        int repeat = parseInt(p.get("repeat"), 0);
        long interval = Math.max(1, parseInt(p.get("repeatinterval"), 1));
        for (int i = 1; i <= repeat; i++) {
            Tasks.runLater(engine.plugin(), call.context().caster(), i * interval, () -> {
                for (Target again : engine.targeters().resolveAll(step.targeter(), step.targeterParams(), call.context())) {
                    Location at = again.location().clone().add(0, yOffset, 0);
                    Tasks.runOwnedAt(engine.plugin(), at, () -> ParticleSupport.spawn(engine.plugin(), at, particle, p));
                }
            });
        }
    }
}
