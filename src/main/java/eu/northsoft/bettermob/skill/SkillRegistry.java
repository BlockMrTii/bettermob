package eu.northsoft.bettermob.skill;

import eu.northsoft.bettermob.BetterMobPlugin;
import eu.northsoft.bettermob.pack.PackScanner;
import eu.northsoft.bettermob.pack.YamlFiles;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

public final class SkillRegistry {
    private final BetterMobPlugin plugin;
    private final File folder;
    private final PackScanner packScanner;
    private volatile Map<String, SkillDefinition> skills = Map.of();

    public SkillRegistry(BetterMobPlugin plugin, PackScanner packScanner) {
        this.plugin = plugin;
        this.packScanner = packScanner;
        this.folder = new File(plugin.getDataFolder(), "skills");
    }

    public void load() {
        if (!folder.exists()) {
            folder.mkdirs();
            plugin.saveResource("skills/chew_wood.yml", false);
            plugin.saveResource("skills/wumpus_wave.yml", false);
        }

        Map<String, SkillDefinition> loaded = new LinkedHashMap<>();
        for (File sourceFolder : packScanner.foldersFor("skills")) {
            for (File file : YamlFiles.collect(sourceFolder)) {
                YamlConfiguration config = YamlConfiguration.loadConfiguration(file);
                for (String id : config.getKeys(false)) {
                    ConfigurationSection section = config.getConfigurationSection(id);
                    if (section == null) continue;
                    if (loaded.containsKey(id.toLowerCase(Locale.ROOT))) {
                        plugin.messages().warn("skill.duplicate", "skill", id, "folder", sourceFolder.getPath());
                        continue;
                    }
                    loaded.put(id.toLowerCase(Locale.ROOT), parse(id, section));
                }
            }
        }
        skills = loaded;
        plugin.messages().info("skill.loaded", "count", loaded.size());
    }

    public SkillDefinition get(String id) {
        return skills.get(id.toLowerCase(Locale.ROOT));
    }

    public Set<String> ids() {
        return skills.keySet();
    }

    private SkillDefinition parse(String id, ConfigurationSection section) {
        List<String> conditions = stringList(section, "Conditions", "Condition");
        List<String> targetConditions = stringList(section, "TargetConditions", "TargetCondition");
        List<SkillStep> steps = new ArrayList<>();
        for (String line : stringList(section, "Skills", "Skill")) {
            SkillStep step = SkillStep.parse(line);
            if (step != null) steps.add(step);
            else plugin.messages().warn("skill.lineInvalid", "skill", id, "line", line);
        }
        return new SkillDefinition(id, conditions, targetConditions, steps, section.getDouble("Cooldown", 0), parseArguments(section.getConfigurationSection("Arguments")));
    }

    private static Map<String, String> parseArguments(ConfigurationSection section) {
        if (section == null) return Map.of();
        Map<String, String> arguments = new LinkedHashMap<>();
        for (String key : section.getKeys(false)) {
            Object value = section.get(key);
            if (value != null && !(value instanceof ConfigurationSection)) arguments.put(key.toLowerCase(Locale.ROOT), String.valueOf(value));
        }
        return Map.copyOf(arguments);
    }

    private static List<String> stringList(ConfigurationSection section, String pluralKey, String singularKey) {
        if (section.isList(pluralKey)) return section.getStringList(pluralKey);
        if (section.isList(singularKey)) return section.getStringList(singularKey);
        return List.of();
    }
}
