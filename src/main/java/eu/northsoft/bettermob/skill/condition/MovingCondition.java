package eu.northsoft.bettermob.skill.condition;

import eu.northsoft.bettermob.skill.SkillContext;
import eu.northsoft.bettermob.skill.Target;

public final class MovingCondition implements SkillCondition {
    private static final double MIN_SPEED_SQUARED = 0.0004;

    @Override
    public boolean test(SkillContext context, String paramsRaw, Target targetOverride) {
        return context.caster().getVelocity().lengthSquared() > MIN_SPEED_SQUARED;
    }
}
