package eu.northsoft.bettermob.skill.mechanic;

import eu.northsoft.bettermob.skill.SkillContext;
import eu.northsoft.bettermob.skill.SkillEngine;
import eu.northsoft.bettermob.skill.Variables;

import java.util.Map;

import static eu.northsoft.bettermob.skill.Params.firstParam;

public final class VariableMechanic implements Mechanic {
    private final SkillEngine engine;
    private final boolean add;

    public VariableMechanic(SkillEngine engine, boolean add) {
        this.engine = engine;
        this.add = add;
    }

    @Override
    public void execute(MechanicCall call) {
        String variable = firstParam(call.params(), "var", "variable", "name");
        String value = firstParam(call.params(), "value", "val", "v", "amount", "a");
        if (variable == null || value == null) return;
        String name = Variables.nameOf(variable);
        if (name == null) {
            engine.plugin().messages().warn("skill.variableName", "variable", variable);
            return;
        }
        SkillContext context = call.context();
        Map<String, String> scope = Variables.isCasterScope(variable)
                ? engine.state().variablesOf(context.caster().getUniqueId()) : context.variables();

        String result = add ? Variables.add(scope.get(name), value)
                : Variables.normalise(value, Variables.typeOf(call.params().get("type")));
        if (result == null) {
            engine.plugin().messages().warn("skill.variableValue", "variable", variable, "value", value);
            return;
        }
        if (!Variables.put(scope, name, result)) {
            engine.plugin().messages().warn("skill.variableLimit", "variable", variable, "limit", Variables.MAX_PER_SCOPE);
        }
    }
}
