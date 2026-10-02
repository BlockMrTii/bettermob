package eu.northsoft.bettermob;

import java.util.List;

final class SkillDefinition {
    final String id;
    final List<String> conditions;
    final List<String> targetConditions;
    final List<SkillStep> steps;

    final double cooldown;

    SkillDefinition(String id, List<String> conditions, List<String> targetConditions, List<SkillStep> steps,
                    double cooldown) {
        this.id = id;
        this.conditions = conditions;
        this.targetConditions = targetConditions;
        this.steps = steps;
        this.cooldown = cooldown;
    }
}
