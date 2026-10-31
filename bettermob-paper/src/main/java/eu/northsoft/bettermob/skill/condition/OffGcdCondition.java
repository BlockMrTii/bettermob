package eu.northsoft.bettermob.skill.condition;

import eu.northsoft.bettermob.skill.SkillContext;
import eu.northsoft.bettermob.skill.SkillEngine;
import eu.northsoft.bettermob.skill.Target;

public final class OffGcdCondition implements SkillCondition {
    private final SkillEngine engine;

    public OffGcdCondition(SkillEngine engine) {
        this.engine = engine;
    }

    @Override
    public boolean test(SkillContext context, String paramsRaw, Target targetOverride) {
        return !engine.state().hasActiveGcd(context.caster().getUniqueId());
    }
}
