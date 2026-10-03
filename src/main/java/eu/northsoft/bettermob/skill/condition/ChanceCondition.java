package eu.northsoft.bettermob.skill.condition;

import eu.northsoft.bettermob.skill.SkillContext;
import eu.northsoft.bettermob.skill.Target;

import java.util.concurrent.ThreadLocalRandom;

import static eu.northsoft.bettermob.skill.Params.conditionParam;
import static eu.northsoft.bettermob.skill.Params.parseFloat;

public final class ChanceCondition implements SkillCondition {
    @Override
    public boolean test(SkillContext context, String paramsRaw, Target targetOverride) {
        return ThreadLocalRandom.current().nextDouble() < parseFloat(conditionParam(paramsRaw, "chance", "c"), 1f);
    }
}
