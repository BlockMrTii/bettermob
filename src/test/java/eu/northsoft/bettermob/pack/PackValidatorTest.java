package eu.northsoft.bettermob.pack;

import eu.northsoft.bettermob.skill.target.CandidateFilters;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PackValidatorTest {
    private static final Set<String> MECHANICS = Set.of("sound", "skill", "potion", "randomskill", "cancelevent", "totem", "damage");
    private static final Set<String> CONDITIONS = Set.of("chance", "hasaura");
    private static final Set<String> TARGETERS = Set.of("self", "target", "entitiesnearorigin");
    private static final Set<String> SKILLS = Set.of("known_skill");

    private final PackValidator validator = new PackValidator(new PackValidator.Knowledge(
            MECHANICS::contains, CONDITIONS::contains, TARGETERS::contains, SKILLS::contains,
            name -> CandidateFilters.knows(name, CONDITIONS::contains)));

    private static void write(Path root, String file, String content) throws IOException {
        Path path = root.resolve(file);
        Files.createDirectories(path.getParent());
        Files.writeString(path, content);
    }

    @Test
    void aCleanPackHasNoIssues(@TempDir Path root) throws IOException {
        write(root, "Mobs/pig.yml", "pig:\n  Skills:\n    - sound{s=entity.pig.hurt} @self ~onDamaged\n    - skill{s=known_skill} @target ~onSpawn\n");
        write(root, "Skills/known_skill.yml", "known_skill:\n  Conditions:\n    - chance{chance=0.5}\n  Skills:\n    - potion{t=SLOW;d=20}\n");
        PackValidator.Report report = validator.validatePack("clean", root.toFile());
        assertEquals(4, report.lines());
        assertTrue(report.issues().isEmpty(), report.issues().toString());
    }

    @Test
    void unsupportedPartsAreReportedWithTheirReason(@TempDir Path root) throws IOException {
        write(root, "mobs/boss.yml", "boss:\n  Skills:\n"
                + "    - warp{x=1} @self ~onSpawn\n"
                + "    - sound{s=a} @nobody ~onSpawn\n"
                + "    - sound{s=a} @self ~onDamaged ?weather{w=rain}\n"
                + "    - skill{s=missing_skill} @self ~onDeath\n"
                + "    - this is not a skill line\n");
        PackValidator.Report report = validator.validatePack("boss", root.toFile());
        assertEquals(5, report.lines());
        assertEquals(PackValidator.Reason.MECHANIC, report.issues().get(0).reason());
        assertEquals("warp", report.issues().get(0).name());
        assertEquals(PackValidator.Reason.TARGETER, report.issues().get(1).reason());
        assertEquals(PackValidator.Reason.CONDITION, report.issues().get(2).reason());
        assertEquals(PackValidator.Reason.SKILL, report.issues().get(3).reason());
        assertEquals(PackValidator.Reason.UNPARSEABLE, report.issues().get(4).reason());
    }

    @Test
    void inlineSkillsAndTotemLinesAreChecked(@TempDir Path root) throws IOException {
        write(root, "skills/bomb.yml", "bomb:\n  Skills:\n"
                + "    - totem{md=2;os=[ - explode{y=2} - damage{a=1} @EntitiesNearOrigin{r=4;Conditions=[ - isPlayer{} true - isWizard{} true ]} ]}\n"
                + "    - skill{s=[ - launch{v=2} ]}\n");
        PackValidator.Report report = validator.validatePack("bomb", root.toFile());
        assertEquals(3, report.issues().size(), report.issues().toString());
        assertEquals("explode", report.issues().get(0).name());
        assertEquals("iswizard", report.issues().get(1).name());
        assertEquals(PackValidator.Reason.TARGETER_CONDITION, report.issues().get(1).reason());
        assertEquals("launch", report.issues().get(2).name());
    }

    @Test
    void dropTablesAndMobDropListsAreChecked(@TempDir Path root) throws IOException {
        write(root, "DropTables/loot.yml", "loot:\n  Drops:\n    - diamond 1to2 0.5\n    - totally broken line here now\n");
        write(root, "mobs/zombie.yml", "zombie:\n  Drops:\n    - loot\n    - bone 1 x\n");
        PackValidator.Report report = validator.validatePack("drops", root.toFile());
        assertEquals(4, report.lines());
        assertEquals(2, report.issues().size(), report.issues().toString());
        assertEquals(PackValidator.Reason.UNPARSEABLE, report.issues().get(0).reason());
    }

    @Test
    void targeterConditionsAcceptSkillConditionsAndTheEntityOnes(@TempDir Path root) throws IOException {
        write(root, "skills/ring.yml", "ring:\n  Skills:\n"
                + "    - damage{a=1} @EntitiesNearOrigin{r=4;Conditions=[ - isPlayer{} true - chance{chance=0.5} true - ismob{} false ]}\n");
        PackValidator.Report report = validator.validatePack("ring", root.toFile());
        assertTrue(report.issues().isEmpty(), report.issues().toString());
    }

    @Test
    void foldersAreMatchedCaseInsensitivelyAndMissingOnesAreSkipped(@TempDir Path root) throws IOException {
        write(root, "SKILLS/a.yml", "a:\n  Skills:\n    - sound{s=a}\n");
        PackValidator.Report report = validator.validatePack("case", root.toFile());
        assertEquals(1, report.lines());
        assertTrue(report.issues().isEmpty());
    }
}
