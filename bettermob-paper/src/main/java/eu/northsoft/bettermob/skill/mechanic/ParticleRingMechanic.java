package eu.northsoft.bettermob.skill.mechanic;

import eu.northsoft.bettermob.skill.SkillEngine;
import org.bukkit.Location;
import org.bukkit.Particle;

import java.util.Map;

import static eu.northsoft.bettermob.skill.Params.firstParam;
import static eu.northsoft.bettermob.skill.Params.parseFloat;
import static eu.northsoft.bettermob.skill.Params.parseInt;

public final class ParticleRingMechanic implements Mechanic {
    private final SkillEngine engine;

    public ParticleRingMechanic(SkillEngine engine) {
        this.engine = engine;
    }

    @Override
    public void execute(MechanicCall call) {
        Map<String, String> p = call.params();
        Particle particle = ParticleSupport.parse(engine.plugin(), firstParam(p, "particle", "p"));
        if (particle == null) return;
        double radius = parseFloat(p.get("radius"), 1f);
        int points = Math.max(1, parseInt(p.get("points"), 8));
        Location center = call.target().location();
        for (int i = 0; i < points; i++) {
            double angle = 2 * Math.PI * i / points;
            ParticleSupport.spawn(engine.plugin(), center.clone().add(Math.cos(angle) * radius, 0, Math.sin(angle) * radius), particle, p);
        }
    }
}
