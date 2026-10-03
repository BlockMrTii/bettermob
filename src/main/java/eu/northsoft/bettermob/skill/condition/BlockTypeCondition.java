package eu.northsoft.bettermob.skill.condition;

import eu.northsoft.bettermob.skill.SkillContext;
import eu.northsoft.bettermob.skill.Target;

public final class BlockTypeCondition implements SkillCondition {
    @Override
    public boolean test(SkillContext context, String paramsRaw, Target targetOverride) {
        return targetOverride != null && targetOverride.block() != null
                && BlockTypes.contains(paramsRaw, targetOverride.block());
    }
}
