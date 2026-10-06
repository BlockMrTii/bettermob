package eu.northsoft.bettermob.skill;

import java.util.List;
import java.util.Map;

final class SkillDefinition {
    public final String id;
    public final List<String> conditions;
    public final List<String> targetConditions;
    public final List<SkillStep> steps;

    public final double cooldown;
    public final Map<String, String> arguments;

    public SkillDefinition(String id, List<String> conditions, List<String> targetConditions, List<SkillStep> steps,
                    double cooldown) {
        this(id, conditions, targetConditions, steps, cooldown, Map.of());
    }

    public SkillDefinition(String id, List<String> conditions, List<String> targetConditions, List<SkillStep> steps,
                    double cooldown, Map<String, String> arguments) {
        this.id = id;
        this.conditions = conditions;
        this.targetConditions = targetConditions;
        this.steps = steps;
        this.cooldown = cooldown;
        this.arguments = arguments;
    }
}
