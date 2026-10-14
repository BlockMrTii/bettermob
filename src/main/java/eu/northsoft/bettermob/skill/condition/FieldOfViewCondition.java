package eu.northsoft.bettermob.skill.condition;

import eu.northsoft.bettermob.skill.SkillContext;
import eu.northsoft.bettermob.skill.Target;
import org.bukkit.Location;
import org.bukkit.entity.LivingEntity;
import org.bukkit.util.Vector;

import static eu.northsoft.bettermob.skill.Params.conditionParam;
import static eu.northsoft.bettermob.skill.Params.parseFloat;

public final class FieldOfViewCondition implements SkillCondition {
    @Override
    public boolean test(SkillContext context, String paramsRaw, Target targetOverride) {
        Location target = targetOverride != null && targetOverride.location() != null ? targetOverride.location()
                : context.trigger() != null ? context.trigger().getLocation() : null;
        if (target == null) return false;
        LivingEntity caster = context.caster();
        if (!target.getWorld().equals(caster.getWorld())) return false;
        Vector toTarget = target.toVector().subtract(caster.getEyeLocation().toVector());
        if (toTarget.lengthSquared() < 1.0e-9) return true;
        double angle = Math.toDegrees(caster.getEyeLocation().getDirection().angle(toTarget));
        return angle <= parseFloat(conditionParam(paramsRaw, "a", "angle", "fov"), 90f) / 2.0;
    }
}
