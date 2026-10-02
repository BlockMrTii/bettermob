package eu.northsoft.bettermob;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

final class SkillRegistry {
    private final BetterMobPlugin plugin;
    private final File folder;
    private final PackScanner packScanner;
    private final Map<String, SkillDefinition> skills = new LinkedHashMap<>();

    SkillRegistry(BetterMobPlugin plugin, PackScanner packScanner) {
        this.plugin = plugin;
        this.packScanner = packScanner;
        this.folder = new File(plugin.getDataFolder(), "skills");
    }

    void load() {
        if (!folder.exists()) {
            folder.mkdirs();
            plugin.saveResource("skills/chew_wood.yml", false);
            plugin.saveResource("skills/wumpus_wave.yml", false);
        }

        skills.clear();
        for (File sourceFolder : packScanner.foldersFor("skills")) {
            for (File file : YamlFiles.collect(sourceFolder)) {
                YamlConfiguration config = YamlConfiguration.loadConfiguration(file);
                for (String id : config.getKeys(false)) {
                    ConfigurationSection section = config.getConfigurationSection(id);
                    if (section == null) continue;
                    if (skills.containsKey(id.toLowerCase(Locale.ROOT))) {
                        plugin.getLogger().warning("Skill '" + id + "' aus " + sourceFolder.getPath() + " ueberschreibt eine bereits geladene Definition - ignoriert.");
                        continue;
                    }
                    skills.put(id.toLowerCase(Locale.ROOT), parse(id, section));
                }
            }
        }
        plugin.getLogger().info(skills.size() + " Skills geladen.");
    }

    SkillDefinition get(String id) {
        return skills.get(id.toLowerCase(Locale.ROOT));
    }

    Set<String> ids() {
        return skills.keySet();
    }

    private SkillDefinition parse(String id, ConfigurationSection section) {
        List<String> conditions = stringList(section, "Conditions", "Condition");
        List<String> targetConditions = stringList(section, "TargetConditions", "TargetCondition");
        List<SkillStep> steps = new ArrayList<>();
        for (String line : stringList(section, "Skills", "Skill")) {
            SkillStep step = SkillStep.parse(line);
            if (step != null) steps.add(step);
            else plugin.getLogger().warning("Skill '" + id + "': Zeile '" + line + "' konnte nicht geparst werden.");
        }
        return new SkillDefinition(id, conditions, targetConditions, steps, section.getDouble("Cooldown", 0));
    }

    private static List<String> stringList(ConfigurationSection section, String pluralKey, String singularKey) {
        if (section.isList(pluralKey)) return section.getStringList(pluralKey);
        if (section.isList(singularKey)) return section.getStringList(singularKey);
        return List.of();
    }
}
