package eu.northsoft.bettermob.skill.condition;

import eu.northsoft.bettermob.skill.SkillContext;
import eu.northsoft.bettermob.skill.SkillEngine;
import eu.northsoft.bettermob.skill.Target;

import static eu.northsoft.bettermob.skill.Params.conditionParam;

public final class HasAuraCondition implements SkillCondition {
    private final SkillEngine engine;

    public HasAuraCondition(SkillEngine engine) {
        this.engine = engine;
    }

    @Override
    public boolean test(SkillContext context, String paramsRaw, Target targetOverride) {
        return engine.state().hasAura(context.caster(), conditionParam(paramsRaw, "n", "name", "aura", "auraname"));
    }
}
