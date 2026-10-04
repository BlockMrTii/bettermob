package eu.northsoft.bettermob.pack;

import eu.northsoft.bettermob.skill.condition.ConditionRegistry;
import eu.northsoft.bettermob.skill.mechanic.BuiltinMechanics;
import eu.northsoft.bettermob.skill.target.CandidateFilters;
import eu.northsoft.bettermob.skill.mechanic.MechanicRegistry;
import eu.northsoft.bettermob.skill.target.TargeterRegistry;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SamplePacksTest {
    private static final Path PACKS = Path.of("src/test/resources/packs");

    @Test
    void everySamplePackUsesOnlySupportedMechanicsConditionsAndTargeters() throws IOException {
        MechanicRegistry mechanics = new MechanicRegistry(null);
        BuiltinMechanics.registerAll(mechanics, null);
        ConditionRegistry conditions = new ConditionRegistry(null);
        TargeterRegistry targeters = new TargeterRegistry(null);

        List<File> packs;
        try (Stream<Path> list = Files.list(PACKS)) {
            packs = list.filter(Files::isDirectory).map(Path::toFile).sorted().toList();
        }
        assertFalse(packs.isEmpty(), "no sample packs found");

        Set<String> skillIds = skillIdsOf(packs);
        PackValidator validator = new PackValidator(new PackValidator.Knowledge(
                name -> mechanics.get(name) != null || name.equals("cancelskill") || name.equals("delay"),
                conditions::has, targeters::has, id -> skillIds.contains(id.toLowerCase(Locale.ROOT)),
                name -> CandidateFilters.knows(name, conditions::has)));

        StringBuilder problems = new StringBuilder();
        int lines = 0;
        for (File pack : packs) {
            PackValidator.Report report = validator.validatePack(pack.getName(), pack);
            lines += report.lines();
            for (PackValidator.Issue issue : report.issues()) {
                if (issue.reason() == PackValidator.Reason.SKILL) continue;
                problems.append(pack.getName()).append(" / ").append(issue.file()).append(": ").append(issue.line())
                        .append("  [").append(issue.reason()).append(' ').append(issue.name()).append("]\n");
            }
        }
        assertTrue(lines > 0, "the sample packs contain no lines");
        assertTrue(problems.length() == 0, problems::toString);
    }

    private static Set<String> skillIdsOf(List<File> packs) {
        Set<String> ids = new HashSet<>();
        for (File pack : packs) {
            File skills = PackScanner.subfolder(pack, "skills");
            if (skills == null) continue;
            for (File file : YamlFiles.collect(skills)) {
                YamlConfiguration config = YamlConfiguration.loadConfiguration(file);
                for (String id : config.getKeys(false)) {
                    ConfigurationSection section = config.getConfigurationSection(id);
                    if (section != null) ids.add(id.toLowerCase(Locale.ROOT));
                }
            }
        }
        return ids;
    }
}
