package eu.northsoft.bettermob.skill.condition;

import eu.northsoft.bettermob.skill.SkillContext;
import eu.northsoft.bettermob.skill.Target;
import org.bukkit.entity.AnimalTamer;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Tameable;

import static eu.northsoft.bettermob.skill.SkillTags.OWNER_PREFIX;

public final class OwnerCondition implements SkillCondition {
    @Override
    public boolean test(SkillContext context, String paramsRaw, Target targetOverride) {
        Entity entity = targetOverride != null && targetOverride.entity() != null ? targetOverride.entity() : context.trigger();
        if (entity == null) return false;
        if (context.caster().getScoreboardTags().contains(OWNER_PREFIX + entity.getUniqueId())) return true;
        if (context.caster() instanceof Tameable tameable) {
            AnimalTamer owner = tameable.getOwner();
            return owner != null && owner.getUniqueId().equals(entity.getUniqueId());
        }
        return false;
    }
}
