package eu.northsoft.bettermob.mob;

import eu.northsoft.bettermob.skill.SkillStep;
import org.bukkit.configuration.ConfigurationSection;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public record MobPhase(HealthSpec health, String display, String model, List<SkillStep> start,
                       Map<MobDefinition.SkillTrigger.Trigger, List<MobDefinition.SkillTrigger>> triggers, boolean replace) {
    private static final Pattern HEALTH = Pattern.compile("^\\s*(<=|>=|<|>)\\s*(\\d+(?:\\.\\d+)?)\\s*(%)?\\s*$");

    public List<MobDefinition.SkillTrigger> triggersOf(MobDefinition.SkillTrigger.Trigger type) {
        return triggers.getOrDefault(type, List.of());
    }

    public static HealthSpec parseHealth(String text) {
        if (text == null) return null;
        Matcher matcher = HEALTH.matcher(text);
        return matcher.matches() ? HealthSpec.parse(matcher.group(1), matcher.group(2), matcher.group(3)) : null;
    }

    public static int select(List<MobPhase> phases, double health, double max) {
        for (int i = phases.size() - 1; i >= 0; i--) {
            if (phases.get(i).health().matches(health, max)) return i;
        }
        return -1;
    }

    public static List<MobPhase> parse(ConfigurationSection mob, Consumer<String> invalid) {
        List<MobPhase> phases = new ArrayList<>();
        List<Map<?, ?>> entries = mob.getMapList("Phases");
        for (int index = 0; index < entries.size(); index++) {
            Map<?, ?> entry = entries.get(index);
            HealthSpec health = parseHealth(entry.get("Health") == null ? null : String.valueOf(entry.get("Health")));
            if (health == null) {
                invalid.accept("Phases[" + index + "]: Health " + entry.get("Health"));
                continue;
            }
            List<SkillStep> start = new ArrayList<>();
            for (String line : lines(entry.get("OnStart"))) {
                SkillStep step = SkillStep.parse(line);
                if (step == null) invalid.accept("Phases[" + index + "]: OnStart " + line);
                else start.add(step);
            }
            Map<MobDefinition.SkillTrigger.Trigger, List<MobDefinition.SkillTrigger>> byType = new EnumMap<>(MobDefinition.SkillTrigger.Trigger.class);
            for (String line : lines(entry.get("Skills"))) {
                MobDefinition.SkillTrigger trigger = MobDefinition.SkillTrigger.parse(line);
                if (trigger == null) invalid.accept("Phases[" + index + "]: Skills " + line);
                else byType.computeIfAbsent(trigger.trigger(), key -> new ArrayList<>()).add(trigger);
            }
            byType.replaceAll((key, list) -> List.copyOf(list));
            phases.add(new MobPhase(health, text(entry.get("Display")), text(entry.get("Model")), List.copyOf(start), byType,
                    "true".equalsIgnoreCase(String.valueOf(entry.get("Replace")))));
        }
        return List.copyOf(phases);
    }

    private static String text(Object value) {
        return value == null || String.valueOf(value).isBlank() ? null : String.valueOf(value);
    }

    private static List<String> lines(Object value) {
        List<String> lines = new ArrayList<>();
        if (value instanceof List<?> list) {
            for (Object line : list) lines.add(String.valueOf(line));
        }
        return lines;
    }
}
