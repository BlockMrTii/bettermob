package eu.northsoft.bettermob.skill;

import eu.northsoft.bettermob.drop.DropEntry;
import eu.northsoft.bettermob.mob.MobDefinition.SkillTrigger;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Loads the YAML files shipped with the plugin plus every pack under {@code src/test/resources/packs}
 * and checks that each mob, item, skill and drop table line parses. Drop a pack in there (any layout,
 * the folder names Mobs, Items, Skills and DropTables decide how a file is read) to cover it.
 */
class PackFilesTest {
    private static final List<Path> ROOTS = List.of(Path.of("src/main/resources"), Path.of("src/test/resources/packs"));

    @Test
    void everyLineOfEveryPackFileParses() throws IOException {
        List<String> failures = new ArrayList<>();
        int files = 0;
        for (Path root : ROOTS) {
            if (!Files.isDirectory(root)) continue;
            try (Stream<Path> walk = Files.walk(root)) {
                for (Path file : walk.filter(p -> p.toString().endsWith(".yml")).toList()) {
                    files += check(root, file, failures);
                }
            }
        }
        assertTrue(files > 0, "no pack files found");
        assertTrue(failures.isEmpty(), () -> String.join("\n", failures));
    }

    @Test
    void shippedSkillsKeepTheirSteps() {
        YamlConfiguration config = YamlConfiguration.loadConfiguration(Path.of("src/main/resources/skills/wumpus_wave.yml").toFile());
        List<String> lines = config.getStringList("nm_wumpus_wave_activate.Skills");
        assertFalse(lines.isEmpty());
        assertTrue(lines.stream().allMatch(line -> SkillStep.parse(line) != null));
    }

    private static int check(Path root, Path file, List<String> failures) {
        String kind = kindOf(root.relativize(file));
        if (kind == null) return 0;
        YamlConfiguration config = YamlConfiguration.loadConfiguration(file.toFile());
        for (String id : config.getKeys(false)) {
            ConfigurationSection section = config.getConfigurationSection(id);
            if (section == null) continue;
            String where = file + " -> " + id + ": ";
            switch (kind) {
                case "mobs", "items" -> section.getStringList("Skills").forEach(line -> {
                    if (SkillTrigger.parse(line) == null) failures.add(where + "trigger '" + line + "'");
                });
                case "skills" -> {
                    section.getStringList("Skills").forEach(line -> {
                        if (SkillStep.parse(line) == null) failures.add(where + "step '" + line + "'");
                    });
                    for (String key : new String[]{"Conditions", "TargetConditions"}) {
                        section.getStringList(key).forEach(line -> {
                            if (Condition.parse(line) == null) failures.add(where + "condition '" + line + "'");
                        });
                    }
                }
                case "droptables" -> section.getStringList("Drops").forEach(line -> {
                    if (DropEntry.parse(line) == null) failures.add(where + "drop '" + line + "'");
                });
                default -> {}
            }
        }
        return 1;
    }

    /** Which kind of definitions a file holds, taken from the closest folder named like one. */
    private static String kindOf(Path relative) {
        for (int i = relative.getNameCount() - 2; i >= 0; i--) {
            String name = relative.getName(i).toString().toLowerCase(Locale.ROOT);
            if (List.of("mobs", "items", "skills", "droptables").contains(name)) return name;
        }
        return null;
    }
}
