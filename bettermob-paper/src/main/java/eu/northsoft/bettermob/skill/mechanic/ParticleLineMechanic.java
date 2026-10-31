package eu.northsoft.bettermob.skill.mechanic;

import eu.northsoft.bettermob.skill.SkillEngine;
import eu.northsoft.bettermob.util.Tasks;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.util.Vector;

import java.util.List;
import java.util.Map;

import static eu.northsoft.bettermob.skill.Params.firstParam;
import static eu.northsoft.bettermob.skill.Params.parseFloat;
import static eu.northsoft.bettermob.skill.Params.parseInt;

public final class ParticleLineMechanic implements Mechanic {
    private static final int MAX_BEAM_TICKS = 100;

    private final SkillEngine engine;
    private final boolean beam;

    public ParticleLineMechanic(SkillEngine engine, boolean beam) {
        this.engine = engine;
        this.beam = beam;
    }

    @Override
    public boolean runsOnTarget() {
        return false;
    }

    @Override
    public void execute(MechanicCall call) {
        Map<String, String> p = call.params();
        Particle particle = ParticleSupport.parse(engine.plugin(), firstParam(p, "particle", "p"));
        if (particle == null) return;
        LivingEntity caster = call.context().caster();
        Location from = caster.getLocation().add(0, caster.getHeight() * 0.6 + parseFloat(p.get("fy"), 0f), 0);
        Location target = call.target().ownedLocation();
        if (target == null) return;
        Location to = target.clone();
        Entity entity = call.target().entity();
        to.add(0, (entity == null ? 0 : entity.getHeight() * 0.6) + parseFloat(firstParam(p, "y", "ty"), 0f), 0);
        if (from.getWorld() == null || !from.getWorld().equals(to.getWorld())) return;

        Vector start = from.toVector();
        Vector end = to.toVector();
        int points = p.containsKey("points") ? ParticleShapes.clamp(parseInt(p.get("points"), 20))
                : ParticleShapes.linePoints(start, end, parseFloat(p.get("density"), 2f));
        List<Vector> shape = ParticleShapes.line(start, end, points);

        if (!beam) {
            draw(from, shape, 0, shape.size(), particle, p);
            return;
        }
        int duration = Math.max(1, Math.min(MAX_BEAM_TICKS, parseInt(firstParam(p, "d", "duration"), 10)));
        int drawn = 0;
        for (int tick = 1; tick <= duration; tick++) {
            int until = (int) Math.ceil((double) shape.size() * tick / duration);
            int first = drawn;
            drawn = until;
            if (until <= first) continue;
            Tasks.runLater(engine.plugin(), caster, tick - 1L, () -> draw(from, shape, first, until, particle, p));
        }
    }

    private void draw(Location origin, List<Vector> shape, int first, int until, Particle particle, Map<String, String> p) {
        Tasks.runOwnedAt(engine.plugin(), origin, () -> {
            for (int i = first; i < until; i++) {
                Vector point = shape.get(i);
                ParticleSupport.spawn(engine.plugin(), new Location(origin.getWorld(), point.getX(), point.getY(), point.getZ()), particle, p);
            }
        });
    }
}
