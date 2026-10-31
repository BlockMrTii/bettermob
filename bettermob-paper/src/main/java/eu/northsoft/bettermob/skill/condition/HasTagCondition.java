package eu.northsoft.bettermob.skill.condition;

import eu.northsoft.bettermob.skill.SkillContext;
import eu.northsoft.bettermob.skill.Target;

import static eu.northsoft.bettermob.skill.Params.conditionParam;
import static eu.northsoft.bettermob.skill.SkillTags.TAG_PREFIX;

public final class HasTagCondition implements SkillCondition {
    @Override
    public boolean test(SkillContext context, String paramsRaw, Target targetOverride) {
        return context.caster().getScoreboardTags().contains(TAG_PREFIX + conditionParam(paramsRaw, "t", "tag", "n"));
    }
}
