package eu.northsoft.bettermob.mob;

import eu.northsoft.bettermob.drop.DropEntry;
import eu.northsoft.bettermob.drop.DropTable;
import eu.northsoft.bettermob.platform.ConfigSection;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.Consumer;

/**
 * Platform-neutral parser that converts a {@link ConfigSection} into a {@link MobDefinition}.
 */
public final class MobDefinitionParser {
    private MobDefinitionParser() {}

    /**
     * Extracts individual mob configuration sections from a root configuration.
     * Supports:
     * - Flat mob definition file (contains top-level "Type")
     * - Single wrapper section
     * - Multi-mob definitions file
     */
    public static Map<String, ConfigSection> extractSections(String fileId, ConfigSection root) {
        if (root == null) return Map.of();
        if (root.contains("Type")) return Map.of(fileId, root);

        List<String> keys = List.copyOf(root.getKeys(false));
        if (keys.size() == 1) {
            ConfigSection nested = root.getConfigurationSection(keys.get(0));
            return Map.of(fileId, nested != null ? nested : root);
        }

        Map<String, ConfigSection> result = new LinkedHashMap<>();
        for (String key : keys) {
            ConfigSection nested = root.getConfigurationSection(key);
            if (nested != null) result.put(key, nested);
        }
        return result;
    }

    /**
     * Parses a single MobDefinition from a ConfigSection.
     */
    public static MobDefinition parse(String id, ConfigSection section, Consumer<String> warningHandler) {
        if (section == null) return null;
        Consumer<String> warn = warningHandler != null ? warningHandler : msg -> {};

        String typeStr = section.getString("Type", "ZOMBIE");
        if (typeStr == null || typeStr.isBlank()) {
            warn.accept("mob " + id + " has missing or invalid Type");
            return null;
        }

        MobDefinition.Options options = MobDefinition.Options.parse(
                section.getConfigurationSection("Options"),
                MobDefinition.Options.DEFAULT
        );

        ConfigSection modulesSection = section.getConfigurationSection("Modules");
        boolean threatTable = modulesSection != null && modulesSection.getBoolean("ThreatTable", false);

        return new MobDefinition(
                id,
                typeStr.trim(),
                section.getString("Display", "&f" + id),
                section.getString("Model", id),
                section.getDouble("Health", 20.0),
                section.getDouble("Damage", 2.0),
                section.getBoolean("RemoveAi", false),
                List.copyOf(section.getStringList("AIGoalSelectors")),
                List.copyOf(section.getStringList("AITargetSelectors")),
                options,
                threatTable,
                parseDamageModifiers(id, section.getStringList("DamageModifiers"), warn),
                parseSkillTriggers(id, section.getStringList("Skills"), warn),
                parseDrops(id, section.getStringList("Drops"), warn),
                section.getString("Faction"),
                parseBossBar(id, modulesSection, warn),
                List.copyOf(section.getStringList("Equipment")),
                parseSpawnRule(id, section.getConfigurationSection("Spawn"), warn),
                parseEgg(section.getConfigurationSection("Egg")),
                Behaviour.parse(section, warn),
                MobPhase.parse(section, option -> {
                    int skills = option.indexOf(": Skills ");
                    if (skills >= 0) ExtraTriggers.warnUnparsed(option.substring(skills + 9), () -> warn.accept("mob " + id + " phase invalid option: " + option));
                    else warn.accept("mob " + id + " phase invalid option: " + option);
                })
        );
    }

    private static MobDefinition.EggSettings parseEgg(ConfigSection egg) {
        if (egg == null) return null;
        String material = egg.getString("Material");
        return new MobDefinition.EggSettings(
                material == null ? null : material.trim().toUpperCase(Locale.ROOT),
                egg.getString("Name")
        );
    }

    private static SpawnRule parseSpawnRule(String id, ConfigSection spawn, Consumer<String> warn) {
        if (spawn == null) return null;
        return SpawnRule.parse(spawn, option -> warn.accept("mob " + id + " spawn rule invalid: " + option));
    }

    private static BossBarSettings parseBossBar(String id, ConfigSection modules, Consumer<String> warn) {
        if (modules == null) return null;
        ConfigSection bar = modules.getConfigurationSection("BossBar");
        if (bar == null) return modules.getBoolean("BossBar", false) ? BossBarSettings.defaults() : null;
        String colorName = bar.getString("Color", "RED");
        BossBarColor color = enumOrNull(BossBarColor.class, colorName);
        if (color == null) {
            warn.accept("mob " + id + " boss bar color invalid: " + colorName);
            color = BossBarColor.RED;
        }
        String styleName = bar.getString("Style", "SOLID");
        BossBarStyle style = enumOrNull(BossBarStyle.class, styleName);
        if (style == null) {
            warn.accept("mob " + id + " boss bar style invalid: " + styleName);
            style = BossBarStyle.SOLID;
        }
        return new BossBarSettings(
                bar.getString("Title", BossBarSettings.DEFAULT_TITLE),
                Math.max(1, bar.getDouble("Range", BossBarSettings.DEFAULT_RANGE)),
                color,
                style,
                bar.getBoolean("CreateFog", false),
                bar.getBoolean("DarkenSky", false),
                bar.getBoolean("PlayMusic", false)
        );
    }

    private static <E extends Enum<E>> E enumOrNull(Class<E> type, String value) {
        try {
            return Enum.valueOf(type, value.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException exception) {
            return null;
        }
    }

    private static DropTable parseDrops(String id, List<String> lines, Consumer<String> warn) {
        List<DropEntry> entries = new ArrayList<>();
        for (String line : lines) {
            DropEntry entry = DropEntry.parse(line);
            if (entry == null) warn.accept("mob " + id + " drop line invalid: " + line);
            else entries.add(entry);
        }
        return entries.isEmpty() ? null : DropTable.anonymous(entries);
    }

    private static List<MobDefinition.SkillTrigger> parseSkillTriggers(String id, List<String> entries, Consumer<String> warn) {
        List<MobDefinition.SkillTrigger> triggers = new ArrayList<>();
        for (String entry : entries) {
            MobDefinition.SkillTrigger trigger = MobDefinition.SkillTrigger.parse(entry);
            if (trigger == null) {
                ExtraTriggers.warnUnparsed(entry, () -> warn.accept("mob " + id + " skill line invalid: " + entry));
            } else {
                triggers.add(trigger);
            }
        }
        return triggers;
    }

    private static Map<String, Double> parseDamageModifiers(String id, List<String> entries, Consumer<String> warn) {
        Map<String, Double> modifiers = new LinkedHashMap<>();
        for (String entry : entries) {
            String[] parts = entry.trim().split("\\s+");
            if (parts.length != 2) {
                warn.accept("mob " + id + " damage modifier invalid: " + entry);
                continue;
            }
            String cause = parts[0].toUpperCase(Locale.ROOT);
            try {
                modifiers.put(cause, Double.parseDouble(parts[1]));
            } catch (NumberFormatException exception) {
                warn.accept("mob " + id + " damage modifier multiplier invalid: " + parts[1]);
            }
        }
        return modifiers;
    }
}
