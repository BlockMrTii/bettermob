package eu.northsoft.bettermob.skill.condition;

import eu.northsoft.bettermob.skill.SkillContext;
import eu.northsoft.bettermob.skill.SkillEngine;
import eu.northsoft.bettermob.skill.Target;
import eu.northsoft.bettermob.skill.Variables;

import java.util.Map;

import static eu.northsoft.bettermob.skill.Params.conditionParam;

public final class VariableCondition implements SkillCondition {
    private final SkillEngine engine;

    public VariableCondition(SkillEngine engine) {
        this.engine = engine;
    }

    @Override
    public boolean test(SkillContext context, String paramsRaw, Target targetOverride) {
        String variable = conditionParam(paramsRaw, "var", "variable", "name");
        String name = variable.isEmpty() ? null : Variables.nameOf(variable);
        if (name == null) return false;
        Map<String, String> scope = Variables.isCasterScope(variable)
                ? engine.state().existingVariablesOf(context.caster().getUniqueId()) : context.variables();
        String stored = scope == null ? null : scope.get(name);
        return Variables.matches(conditionParam(paramsRaw, "value", "val", "v"), stored);
    }
}
