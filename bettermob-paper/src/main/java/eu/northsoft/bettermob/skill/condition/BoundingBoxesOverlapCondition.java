package eu.northsoft.bettermob.skill.condition;

import eu.northsoft.bettermob.skill.SkillContext;
import eu.northsoft.bettermob.skill.Target;
import org.bukkit.entity.Entity;

public final class BoundingBoxesOverlapCondition implements SkillCondition {
    @Override
    public boolean test(SkillContext context, String paramsRaw, Target targetOverride) {
        Entity other = targetOverride != null && targetOverride.entity() != null ? targetOverride.entity() : context.trigger();
        if (other == null || other == context.caster() || !other.getWorld().equals(context.caster().getWorld())) return false;
        return context.caster().getBoundingBox().overlaps(other.getBoundingBox());
    }
}
