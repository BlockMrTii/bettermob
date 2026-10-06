package eu.northsoft.bettermob.skill.condition;

import eu.northsoft.bettermob.skill.SkillContext;
import eu.northsoft.bettermob.skill.Target;
import org.bukkit.Bukkit;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Mob;

import static eu.northsoft.bettermob.skill.Params.conditionParam;

public final class DistanceCondition implements SkillCondition {
    @Override
    public boolean test(SkillContext context, String paramsRaw, Target targetOverride) {
        String spec = conditionParam(paramsRaw, "d", "distance");
        LivingEntity other = context.trigger();
        if (other == null && context.caster() instanceof Mob mob) other = mob.getTarget();
        if (other == null || !Bukkit.isOwnedByCurrentRegion(other) || !other.getWorld().equals(context.caster().getWorld())) return false;
        return RangeSpec.matches(spec, other.getLocation().distance(context.caster().getLocation()));
    }
}
