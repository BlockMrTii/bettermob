package eu.northsoft.bettermob.skill.condition;

import eu.northsoft.bettermob.skill.SkillContext;
import eu.northsoft.bettermob.skill.SkillEngine;
import eu.northsoft.bettermob.skill.Target;

import static eu.northsoft.bettermob.skill.Params.conditionParam;

public final class SkillOnCooldownCondition implements SkillCondition {
    private final SkillEngine engine;

    public SkillOnCooldownCondition(SkillEngine engine) {
        this.engine = engine;
    }

    @Override
    public boolean test(SkillContext context, String paramsRaw, Target targetOverride) {
        return engine.state().skillOnCooldown(context.caster(), conditionParam(paramsRaw, "skill", "s", "name"));
    }
}
