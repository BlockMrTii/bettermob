package eu.northsoft.bettermob.skill.condition;

import eu.northsoft.bettermob.skill.SkillContext;
import eu.northsoft.bettermob.skill.Target;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Mob;

public final class LineOfSightCondition implements SkillCondition {
    @Override
    public boolean test(SkillContext context, String paramsRaw, Target targetOverride) {
        LivingEntity other = context.trigger();
        if (other == null && context.caster() instanceof Mob mob) other = mob.getTarget();
        return other != null && other.getWorld().equals(context.caster().getWorld()) && context.caster().hasLineOfSight(other);
    }
}
