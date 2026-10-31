package eu.northsoft.bettermob.skill.mechanic;

import eu.northsoft.bettermob.skill.SkillEngine;
import eu.northsoft.bettermob.util.RegionEntities;
import org.bukkit.Location;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.util.BoundingBox;
import org.bukkit.util.Vector;

import static eu.northsoft.bettermob.skill.Params.firstParam;
import static eu.northsoft.bettermob.skill.Params.parseFloat;

public final class RayTraceToMechanic implements Mechanic {
    private static final double MAX_LENGTH = 64;
    private static final double MAX_WIDTH = 8;

    private final SkillEngine engine;

    public RayTraceToMechanic(SkillEngine engine) {
        this.engine = engine;
    }

    @Override
    public boolean runsOnTarget() {
        return false;
    }

    static boolean crosses(BoundingBox box, Vector start, Vector end, double width) {
        Vector direction = end.clone().subtract(start);
        double length = direction.length();
        if (length < 1.0e-6) return box.clone().expand(width / 2).contains(start);
        return box.clone().expand(width / 2).rayTrace(start, direction.normalize(), length) != null;
    }

    @Override
    public void execute(MechanicCall call) {
        String skill = firstParam(call.params(), "es", "onhit", "hitskill", "hs", "s");
        Location target = call.target().entity() != null ? call.target().ownedLocation() : call.target().location();
        LivingEntity caster = call.context().caster();
        if (skill == null || target == null || !target.getWorld().equals(caster.getWorld())) return;
        Vector start = caster.getLocation().toVector().add(new Vector(0, parseFloat(firstParam(call.params(), "syo", "startyoffset"), 0f), 0));
        Vector end = target.toVector().add(new Vector(0, parseFloat(firstParam(call.params(), "tyo", "targetyoffset"), 0f), 0));
        if (end.distance(start) > MAX_LENGTH) end = start.clone().add(end.clone().subtract(start).normalize().multiply(MAX_LENGTH));
        double width = Math.max(0, Math.min(MAX_WIDTH, parseFloat(firstParam(call.params(), "rw", "raywidth", "width"), 0f)));
        Location middle = start.clone().add(end).multiply(0.5).toLocation(caster.getWorld());
        for (Entity entity : RegionEntities.near(middle, end.distance(start) / 2 + width + 2)) {
            if (!(entity instanceof LivingEntity living) || living == caster || living.isDead() || entity instanceof ArmorStand) continue;
            if (!crosses(living.getBoundingBox(), start, end, width)) continue;
            if (skill.trim().startsWith("[")) engine.runInline(skill, call.context().withTrigger(living));
            else engine.runById(skill.trim(), call.context().withTrigger(living));
        }
    }
}
