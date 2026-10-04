package eu.northsoft.bettermob.skill.condition;

import eu.northsoft.bettermob.skill.SkillContext;
import eu.northsoft.bettermob.skill.Target;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.LivingEntity;

import static eu.northsoft.bettermob.skill.Params.conditionParam;

public final class HealthCondition implements SkillCondition {
    @Override
    public boolean test(SkillContext context, String paramsRaw, Target targetOverride) {
        String spec = conditionParam(paramsRaw, "h", "health");
        LivingEntity caster = context.caster();
        if (spec.endsWith("%")) {
            var maxHealth = caster.getAttribute(Attribute.MAX_HEALTH);
            double max = maxHealth == null ? caster.getHealth() : maxHealth.getValue();
            if (max <= 0) return false;
            return RangeSpec.matches(spec.substring(0, spec.length() - 1), caster.getHealth() / max * 100);
        }
        return RangeSpec.matches(spec, caster.getHealth());
    }
}
