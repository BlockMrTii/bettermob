package eu.northsoft.bettermob.skill.condition;

import eu.northsoft.bettermob.skill.SkillContext;
import eu.northsoft.bettermob.skill.Target;

public final class DayCondition implements SkillCondition {
    @Override
    public boolean test(SkillContext context, String paramsRaw, Target targetOverride) {
        return !TimeCondition.isNight(context.caster().getWorld().getTime());
    }
}
