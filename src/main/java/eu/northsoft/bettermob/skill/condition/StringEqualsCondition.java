package eu.northsoft.bettermob.skill.condition;

import eu.northsoft.bettermob.skill.SkillContext;
import eu.northsoft.bettermob.skill.SkillEngine;
import eu.northsoft.bettermob.skill.Target;

import java.util.Map;

import static eu.northsoft.bettermob.skill.Params.conditionParam;

public final class StringEqualsCondition implements SkillCondition {
    private final SkillEngine engine;

    public StringEqualsCondition(SkillEngine engine) {
        this.engine = engine;
    }

    @Override
    public boolean test(SkillContext context, String paramsRaw, Target targetOverride) {
        Map<String, String> caster = engine.state().existingVariablesOf(context.caster().getUniqueId());
        org.bukkit.entity.Entity entity = targetOverride != null && targetOverride.entity() != null ? targetOverride.entity() : context.trigger();
        Map<String, String> target = entity == null ? null : engine.state().existingVariablesOf(entity.getUniqueId());
        String first = SkillEngine.replaceVariables(conditionParam(paramsRaw, "val1", "value1", "a"), context.variables(), caster, target);
        String second = SkillEngine.replaceVariables(conditionParam(paramsRaw, "val2", "value2", "b"), context.variables(), caster, target);
        return first.equals(second);
    }
}
