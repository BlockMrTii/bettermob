package eu.northsoft.bettermob.skill.condition;

import eu.northsoft.bettermob.skill.SkillContext;
import eu.northsoft.bettermob.skill.SkillEngine;
import eu.northsoft.bettermob.skill.Target;
import eu.northsoft.bettermob.skill.Variables;

import java.util.Map;

import static eu.northsoft.bettermob.skill.Params.conditionParam;

public final class VariableCondition implements SkillCondition {
    public enum Test { EQUALS, IS_SET, IN_RANGE }

    private final SkillEngine engine;
    private final Test test;

    public VariableCondition(SkillEngine engine) {
        this(engine, Test.EQUALS);
    }

    public VariableCondition(SkillEngine engine, Test test) {
        this.engine = engine;
        this.test = test;
    }

    @Override
    public boolean test(SkillContext context, String paramsRaw, Target targetOverride) {
        String variable = conditionParam(paramsRaw, "var", "variable", "name");
        String name = variable.isEmpty() ? null : Variables.nameOf(variable);
        if (name == null) return false;
        Map<String, String> scope = Variables.scopeOf(variable, context, targetOverride == null ? null : targetOverride.entity(), engine.state(), false);
        String stored = scope == null ? null : scope.get(name);
        return switch (test) {
            case IS_SET -> stored != null;
            case IN_RANGE -> Variables.inRange(stored, conditionParam(paramsRaw, "range", "value", "val", "v"));
            case EQUALS -> Variables.matches(conditionParam(paramsRaw, "value", "val", "v"), stored);
        };
    }
}
