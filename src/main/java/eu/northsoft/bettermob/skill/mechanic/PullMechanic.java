package eu.northsoft.bettermob.skill.mechanic;

import eu.northsoft.bettermob.skill.SkillEngine;
import eu.northsoft.bettermob.util.Tasks;
import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.util.Vector;

import static eu.northsoft.bettermob.skill.Params.firstParam;
import static eu.northsoft.bettermob.skill.Params.parseNumber;

public final class PullMechanic implements Mechanic {
    private static final double MAX_SPEED = 4.0;

    private final SkillEngine engine;
    private final boolean propel;

    public PullMechanic(SkillEngine engine, boolean propel) {
        this.engine = engine;
        this.propel = propel;
    }

    @Override
    public boolean runsOnTarget() {
        return false;
    }

    static Vector pullVelocity(Vector from, Vector to, double velocity) {
        Vector pull = to.clone().subtract(from).multiply(velocity / 10.0);
        if (pull.lengthSquared() > MAX_SPEED * MAX_SPEED) pull.normalize().multiply(MAX_SPEED);
        return pull;
    }

    static Vector propelVelocity(Vector from, Vector to, Vector facing, double velocity) {
        Vector direction = to.clone().subtract(from);
        if (direction.lengthSquared() < 1.0e-6) direction = facing.clone();
        if (direction.lengthSquared() < 1.0e-9) return new Vector();
        return direction.normalize().multiply(Math.max(-MAX_SPEED, Math.min(MAX_SPEED, velocity / 10.0)));
    }

    @Override
    public void execute(MechanicCall call) {
        double velocity = parseNumber(firstParam(call.params(), "velocity", "v"), propel ? 10 : 1);
        if (!Double.isFinite(velocity)) return;
        LivingEntity caster = call.context().caster();
        if (propel) {
            Location at = call.target().entity() == caster ? caster.getLocation() : call.target().ownedLocation();
            if (at == null) return;
            Vector to = at.toVector();
            Tasks.runOwned(engine.plugin(), caster, () -> caster.setVelocity(
                    propelVelocity(caster.getLocation().toVector(), to, caster.getLocation().getDirection(), velocity)));
            return;
        }
        Entity pulled = call.target().entity();
        if (pulled == null || pulled == caster) return;
        Location origin = engine.locationParam(firstParam(call.params(), "origin", "o"), call.context());
        Vector to = (origin != null ? origin : caster.getLocation()).toVector();
        Tasks.runOwned(engine.plugin(), pulled, () -> pulled.setVelocity(pullVelocity(pulled.getLocation().toVector(), to, velocity)));
    }
}
