package eu.northsoft.bettermob.skill.condition;

import eu.northsoft.bettermob.skill.SkillContext;
import eu.northsoft.bettermob.skill.Target;

public final class OnBlockCondition implements SkillCondition {
    @Override
    public boolean test(SkillContext context, String paramsRaw, Target targetOverride) {
        return BlockTypes.contains(paramsRaw, context.caster().getLocation().subtract(0, 0.1, 0).getBlock());
    }
}
