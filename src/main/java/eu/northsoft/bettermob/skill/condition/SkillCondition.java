package eu.northsoft.bettermob.skill.condition;

import eu.northsoft.bettermob.skill.SkillContext;
import eu.northsoft.bettermob.skill.Target;

public interface SkillCondition {
    boolean test(SkillContext context, String paramsRaw, Target targetOverride);
}
