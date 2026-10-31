package eu.northsoft.bettermob.mob;

import eu.northsoft.bettermob.item.EquipmentSupport;
import eu.northsoft.bettermob.skill.SkillStep;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public record MobPhase(HealthSpec health, String display, String model, List<SkillStep> start,
                       Map<MobDefinition.SkillTrigger.Trigger, List<MobDefinition.SkillTrigger>> triggers,
                       MobDefinition.Options options, List<String> equipment, Map<?, ?> definition) {
    private static final Pattern HEALTH = Pattern.compile("^\\s*(<=|>=|<|>)\\s*(\\d+(?:\\.\\d+)?)\\s*(%)?\\s*$");
    private static final Set<String> FIELDS = Set.of("Health", "Display", "Model", "OnStart", "Skills", "Options", "Equipment", "Replace");
    private static final Set<String> NUMBERS = Set.of("MovementSpeed", "KnockbackResistance", "FollowRange", "Scale");
    private static final Set<String> FLAGS = Set.of("Collidable", "PreventOtherDrops", "Silent", "PreventRenaming", "PreventLeashing",
            "AlwaysShowName", "PreventSunburn", "Invincible", "Invisible", "CanMove", "Interactable", "Marker", "PreventItemPickup");

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
        MobDefinition.Options base = MobDefinition.Options.parse(mob.getConfigurationSection("Options"), MobDefinition.Options.DEFAULT);
        Object raw = mob.get("Phases");
        if (raw == null) return List.of();
        if (!(raw instanceof List<?> entries)) {
            invalid.accept("Phases: expected a list");
            return List.of();
        }
        for (int index = 0; index < entries.size(); index++) {
            String path = "Phases[" + index + "]: ";
            if (!(entries.get(index) instanceof Map<?, ?> entry)) {
                invalid.accept(path + "expected a phase definition");
                continue;
            }
            validateFields(entry, message -> invalid.accept(path + message));
            HealthSpec health = parseHealth(entry.get("Health") instanceof String text ? text : null);
            if (health == null) {
                invalid.accept(path + "Health " + entry.get("Health"));
                continue;
            }
            List<SkillStep> start = new ArrayList<>();
            for (String line : lines(entry.get("OnStart"))) {
                SkillStep step = SkillStep.parse(line);
                if (step == null) invalid.accept(path + "OnStart " + line);
                else start.add(step);
            }
            Map<MobDefinition.SkillTrigger.Trigger, List<MobDefinition.SkillTrigger>> byType = new EnumMap<>(MobDefinition.SkillTrigger.Trigger.class);
            for (String line : lines(entry.get("Skills"))) {
                MobDefinition.SkillTrigger trigger = MobDefinition.SkillTrigger.parse(line);
                if (trigger != null) byType.computeIfAbsent(trigger.trigger(), key -> new ArrayList<>()).add(trigger);
                else if (!ExtraTriggers.accepts(line)) invalid.accept(path + "Skills " + line);
            }
            byType.replaceAll((key, list) -> List.copyOf(list));
            YamlConfiguration config = new YamlConfiguration();
            ConfigurationSection section = config.createSection("phase", entry);
            phases.add(new MobPhase(health, text(entry.get("Display")), text(entry.get("Model")), List.copyOf(start), Map.copyOf(byType),
                    MobDefinition.Options.parse(section.getConfigurationSection("Options"), base),
                    entry.containsKey("Equipment") ? lines(entry.get("Equipment")) : null, java.util.Collections.unmodifiableMap(new java.util.LinkedHashMap<>(entry))));
        }
        return List.copyOf(phases);
    }

    private static void validateFields(Map<?, ?> entry, Consumer<String> invalid) {
        for (Object key : entry.keySet()) if (!FIELDS.contains(String.valueOf(key))) invalid.accept("unknown field " + key);
        for (String key : List.of("Display", "Model")) {
            if (entry.containsKey(key) && !(entry.get(key) instanceof String)) invalid.accept(key + ": expected text");
        }
        for (String key : List.of("OnStart", "Skills", "Equipment")) {
            if (!entry.containsKey(key)) continue;
            if (!(entry.get(key) instanceof List<?> list) || list.stream().anyMatch(value -> !(value instanceof String))) {
                invalid.accept(key + ": expected a list of strings");
            }
        }
        if (entry.containsKey("Replace") && !isBoolean(entry.get("Replace"))) invalid.accept("Replace: expected a boolean");
        Object options = entry.get("Options");
        if (entry.containsKey("Options")) {
            if (!(options instanceof Map<?, ?> map)) invalid.accept("Options: expected a mapping");
            else map.forEach((key, value) -> {
                String name = String.valueOf(key);
                boolean valid = NUMBERS.contains(name) ? value instanceof Number n && Double.isFinite(n.doubleValue()) && n.doubleValue() >= 0
                        : FLAGS.contains(name) ? isBoolean(value) : name.equals("ItemHead") && value instanceof String;
                if (!valid) invalid.accept("Options." + name + ": " + value);
            });
        }
        for (String line : lines(entry.get("Equipment"))) {
            String[] parts = line.trim().split(":", 2);
            if (parts[0].isBlank() || (parts.length > 1 && !EquipmentSupport.isKnownSlot(parts[1]))) invalid.accept("Equipment " + line);
        }
    }

    private static boolean isBoolean(Object value) {
        return value instanceof Boolean || "true".equalsIgnoreCase(String.valueOf(value)) || "false".equalsIgnoreCase(String.valueOf(value));
    }

    private static String text(Object value) {
        return value instanceof String text && !text.isBlank() ? text : null;
    }

    private static List<String> lines(Object value) {
        return value instanceof List<?> list ? list.stream().filter(String.class::isInstance).map(String.class::cast).toList() : List.of();
    }
}
