package eu.northsoft.bettermob.skill.condition;

import eu.northsoft.bettermob.skill.SkillContext;
import eu.northsoft.bettermob.skill.Target;
import org.bukkit.Location;

public final class OutsideCondition implements SkillCondition {
    @Override
    public boolean test(SkillContext context, String paramsRaw, Target targetOverride) {
        Location at = context.caster().getLocation();
        return at.getWorld().getHighestBlockYAt(at) <= at.getBlockY();
    }
}
