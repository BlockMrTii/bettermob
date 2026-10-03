package eu.northsoft.bettermob.skill.condition;

import eu.northsoft.bettermob.skill.SkillContext;
import eu.northsoft.bettermob.skill.Target;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Mob;

import static eu.northsoft.bettermob.skill.Params.conditionParam;

public final class DistanceCondition implements SkillCondition {
    @Override
    public boolean test(SkillContext context, String paramsRaw, Target targetOverride) {
        String spec = conditionParam(paramsRaw, "d", "distance");
        LivingEntity other = context.trigger();
        if (other == null && context.caster() instanceof Mob mob) other = mob.getTarget();
        if (other == null || !other.getWorld().equals(context.caster().getWorld())) return false;
        double distance = other.getLocation().distance(context.caster().getLocation());
        try {
            if (spec.startsWith(">=")) return distance >= Double.parseDouble(spec.substring(2));
            if (spec.startsWith("<=")) return distance <= Double.parseDouble(spec.substring(2));
            if (spec.startsWith(">")) return distance > Double.parseDouble(spec.substring(1));
            if (spec.startsWith("<")) return distance < Double.parseDouble(spec.substring(1));
            int dash = spec.indexOf('-', 1);
            if (dash > 0) return distance >= Double.parseDouble(spec.substring(0, dash)) && distance <= Double.parseDouble(spec.substring(dash + 1));
            return Math.abs(distance - Double.parseDouble(spec)) < 0.5;
        } catch (NumberFormatException exception) {
            return false;
        }
    }
}
