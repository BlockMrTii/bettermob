package eu.northsoft.bettermob.skill.mechanic;

import eu.northsoft.bettermob.skill.Calculator;
import eu.northsoft.bettermob.skill.SkillContext;
import eu.northsoft.bettermob.skill.SkillEngine;
import eu.northsoft.bettermob.skill.Variables;
import eu.northsoft.bettermob.util.Tasks;
import org.bukkit.entity.Entity;

import java.util.Map;

import static eu.northsoft.bettermob.skill.Params.firstParam;
import static eu.northsoft.bettermob.skill.Params.parseInt;
import static eu.northsoft.bettermob.skill.Params.stripQuotes;

public final class VariableMechanic implements Mechanic {
    public enum Mode { SET, ADD, SUBTRACT, MATH, UNSET }

    private static final int MAX_DURATION_TICKS = 72000;

    private final SkillEngine engine;
    private final Mode mode;

    public VariableMechanic(SkillEngine engine, boolean add) {
        this(engine, add ? Mode.ADD : Mode.SET);
    }

    public VariableMechanic(SkillEngine engine, Mode mode) {
        this.engine = engine;
        this.mode = mode;
    }

    @Override
    public void execute(MechanicCall call) {
        String variable = firstParam(call.params(), "var", "variable", "name");
        if (variable == null) return;
        String name = Variables.nameOf(variable);
        if (name == null) {
            engine.plugin().messages().warn("skill.variableName", "variable", variable);
            return;
        }
        SkillContext context = call.context();
        Entity target = call.target().entity();
        Map<String, String> scope = Variables.scopeOf(variable, context, target, engine.state(), mode != Mode.UNSET);
        if (scope == null) return;
        if (mode == Mode.UNSET) {
            synchronized (scope) {
                scope.remove(name);
            }
            return;
        }

        String result = compute(call, scope.get(name));
        if (result == null) {
            engine.plugin().messages().warn("skill.variableValue", "variable", variable, "value", firstParam(call.params(), "value", "val", "v", "amount", "a", "equation", "expr"));
            return;
        }
        if (!Variables.put(scope, name, result)) {
            engine.plugin().messages().warn("skill.variableLimit", "variable", variable, "limit", Variables.MAX_PER_SCOPE);
            return;
        }
        int duration = Math.min(MAX_DURATION_TICKS, parseInt(firstParam(call.params(), "duration", "d"), 0));
        if (duration > 0) {
            Entity owner = target != null ? target : context.caster();
            Tasks.runLater(engine.plugin(), owner, duration, () -> {
                synchronized (scope) {
                    if (result.equals(scope.get(name))) scope.remove(name);
                }
            });
        }
    }

    private String compute(MechanicCall call, String current) {
        if (mode == Mode.MATH) {
            String equation = firstParam(call.params(), "equation", "expr", "value", "val", "v");
            Double value = equation == null ? null : Calculator.evaluate(stripQuotes(equation));
            return value == null ? null : Variables.formatNumber(value);
        }
        String value = firstParam(call.params(), "value", "val", "v", "amount", "a");
        if (value == null) return null;
        return switch (mode) {
            case ADD -> Variables.add(current, value);
            case SUBTRACT -> Variables.subtract(current, value);
            default -> Variables.normalise(value, Variables.typeOf(call.params().get("type")));
        };
    }
}
