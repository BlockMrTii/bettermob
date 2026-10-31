package eu.northsoft.bettermob.skill.mechanic;

import eu.northsoft.bettermob.skill.SkillEngine;
import eu.northsoft.bettermob.util.Tasks;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.util.Vector;

import java.util.List;
import java.util.Map;

import static eu.northsoft.bettermob.skill.Params.firstParam;
import static eu.northsoft.bettermob.skill.Params.parseFloat;
import static eu.northsoft.bettermob.skill.Params.parseInt;

public final class ParticleShapeMechanic implements Mechanic {
    public enum Shape { SPHERE, HELIX }

    private final SkillEngine engine;
    private final Shape shape;

    public ParticleShapeMechanic(SkillEngine engine, Shape shape) {
        this.engine = engine;
        this.shape = shape;
    }

    @Override
    public void execute(MechanicCall call) {
        Map<String, String> p = call.params();
        Particle particle = ParticleSupport.parse(engine.plugin(), firstParam(p, "particle", "p"));
        if (particle == null) return;
        Location center = call.target().location().clone().add(0, parseFloat(firstParam(p, "y", "yoffset"), 0f), 0);
        double radius = parseFloat(p.get("radius"), shape == Shape.SPHERE ? 1.5f : 1f);
        List<Vector> points = shape == Shape.SPHERE
                ? ParticleShapes.sphere(radius, parseInt(p.get("points"), 40))
                : ParticleShapes.helix(radius, parseFloat(p.get("height"), 2f), parseFloat(p.get("turns"), 3f), parseInt(p.get("points"), 60));
        Tasks.runOwnedAt(engine.plugin(), center, () -> {
            for (Vector offset : points) ParticleSupport.spawn(engine.plugin(), center.clone().add(offset), particle, p);
        });
    }
}
