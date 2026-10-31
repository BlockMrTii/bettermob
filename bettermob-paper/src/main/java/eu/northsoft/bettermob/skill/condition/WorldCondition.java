package eu.northsoft.bettermob.skill.condition;

import eu.northsoft.bettermob.skill.SkillContext;
import eu.northsoft.bettermob.skill.Target;

import static eu.northsoft.bettermob.skill.Params.conditionParam;

public final class WorldCondition implements SkillCondition {
    @Override
    public boolean test(SkillContext context, String paramsRaw, Target targetOverride) {
        String name = context.caster().getWorld().getName();
        for (String candidate : conditionParam(paramsRaw, "w", "world").split(",")) {
            if (candidate.trim().equalsIgnoreCase(name)) return true;
        }
        return false;
    }
}
