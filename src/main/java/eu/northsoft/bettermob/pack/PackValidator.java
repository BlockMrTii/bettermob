package eu.northsoft.bettermob.pack;

import eu.northsoft.bettermob.drop.DropEntry;
import eu.northsoft.bettermob.mob.MobDefinition;
import eu.northsoft.bettermob.skill.Condition;
import eu.northsoft.bettermob.skill.SkillEngine;
import eu.northsoft.bettermob.skill.SkillStep;
import eu.northsoft.bettermob.skill.target.CandidateFilters;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.function.Predicate;

public final class PackValidator {
    public record Knowledge(Predicate<String> mechanic, Predicate<String> condition, Predicate<String> targeter, Predicate<String> skill,
                            Predicate<String> targeterCondition) {}

    public enum Reason { UNPARSEABLE, MECHANIC, TARGETER, CONDITION, TARGETER_CONDITION, SKILL }

    public record Issue(String file, String line, Reason reason, String name) {}

    public static final class Report {
        private final String pack;
        private final List<Issue> issues = new ArrayList<>();
        private int lines;

        Report(String pack) {
            this.pack = pack;
        }

        public String pack() {
            return pack;
        }

        public int lines() {
            return lines;
        }

        public List<Issue> issues() {
            return issues;
        }
    }

    private static final List<String> FOLDERS = List.of("mobs", "skills", "items", "droptables");

    private final Knowledge known;

    public PackValidator(Knowledge known) {
        this.known = known;
    }

    public static Knowledge knowledgeOf(SkillEngine engine, java.util.function.Predicate<String> skill) {
        return new Knowledge(
                name -> engine.mechanics().get(name) != null || name.equals("cancelskill") || name.equals("delay"),
                name -> engine.conditionRegistry().has(name),
                name -> engine.targeters().has(name),
                skill,
                name -> CandidateFilters.knows(name, engine.conditionRegistry()::has));
    }

    public Report validatePack(String pack, File folder) {
        Report report = new Report(pack);
        for (String name : FOLDERS) {
            File sub = PackScanner.subfolder(folder, name);
            if (sub == null) continue;
            for (File file : YamlFiles.collect(sub)) {
                YamlConfiguration config = YamlConfiguration.loadConfiguration(file);
                String label = sub.toPath().getParent().relativize(file.toPath()).toString().replace('\\', '/');
                if (name.equals("skills")) validateSkillFile(config, label, report);
                else if (name.equals("droptables")) validateDropFile(config, label, report);
                else walkTriggerLists(config, label, report);
            }
        }
        return report;
    }

    void walkTriggerLists(ConfigurationSection section, String file, Report report) {
        for (String key : section.getKeys(false)) {
            if (key.equalsIgnoreCase("Skills") && section.isList(key)) {
                for (String line : section.getStringList(key)) validateTriggerLine(line, file, report);
            } else if (key.equalsIgnoreCase("Drops") && section.isList(key)) {
                for (String line : section.getStringList(key)) validateDropLine(line, file, report);
            } else if (section.isConfigurationSection(key)) {
                walkTriggerLists(section.getConfigurationSection(key), file, report);
            }
        }
    }

    void validateDropFile(ConfigurationSection root, String file, Report report) {
        for (String id : root.getKeys(false)) {
            ConfigurationSection table = root.getConfigurationSection(id);
            if (table == null || !table.isList("Drops")) continue;
            for (String line : table.getStringList("Drops")) validateDropLine(line, file, report);
        }
    }

    void validateDropLine(String line, String file, Report report) {
        report.lines++;
        if (DropEntry.parse(line) == null) report.issues.add(new Issue(file, line, Reason.UNPARSEABLE, ""));
    }

    void validateSkillFile(ConfigurationSection root, String file, Report report) {
        for (String id : root.getKeys(false)) {
            ConfigurationSection skill = root.getConfigurationSection(id);
            if (skill == null) continue;
            for (String key : skill.getKeys(false)) {
                if (!skill.isList(key)) continue;
                String lower = key.toLowerCase(Locale.ROOT);
                if (lower.equals("skills") || lower.equals("skill")) {
                    for (String line : skill.getStringList(key)) validateStepLine(line, file, report);
                } else if (lower.equals("conditions") || lower.equals("condition") || lower.equals("targetconditions") || lower.equals("targetcondition")) {
                    for (String line : skill.getStringList(key)) validateConditionLine(line, file, report);
                }
            }
        }
    }

    void validateTriggerLine(String line, String file, Report report) {
        report.lines++;
        MobDefinition.SkillTrigger trigger = MobDefinition.SkillTrigger.parse(line);
        if (trigger == null) {
            report.issues.add(new Issue(file, line, Reason.UNPARSEABLE, ""));
            return;
        }
        checkStep(trigger.step(), line, file, report);
    }

    void validateStepLine(String line, String file, Report report) {
        report.lines++;
        SkillStep step = SkillStep.parse(line);
        if (step == null) {
            report.issues.add(new Issue(file, line, Reason.UNPARSEABLE, ""));
            return;
        }
        checkStep(step, line, file, report);
    }

    void validateConditionLine(String line, String file, Report report) {
        report.lines++;
        checkCondition(line, line, file, report);
    }

    private void checkCondition(String raw, String line, String file, Report report) {
        Condition condition = Condition.parse(raw);
        if (condition == null) report.issues.add(new Issue(file, line, Reason.UNPARSEABLE, ""));
        else if (!known.condition().test(condition.name())) report.issues.add(new Issue(file, line, Reason.CONDITION, condition.name()));
    }

    private void checkStep(SkillStep step, String line, String file, Report report) {
        if (!(step instanceof SkillStep.Mechanic mechanic)) return;
        if (!known.mechanic().test(mechanic.name())) report.issues.add(new Issue(file, line, Reason.MECHANIC, mechanic.name()));
        if (!mechanic.targeter().isEmpty() && !known.targeter().test(mechanic.targeter())) {
            report.issues.add(new Issue(file, line, Reason.TARGETER, mechanic.targeter()));
        }
        if (mechanic.inlineCondition() != null) checkCondition(mechanic.inlineCondition(), line, file, report);

        String conditions = mechanic.targeterParams().get("conditions");
        if (conditions != null) {
            for (String raw : SkillEngine.splitInline(conditions)) {
                Condition condition = Condition.parse(raw);
                if (condition == null) report.issues.add(new Issue(file, line, Reason.UNPARSEABLE, ""));
                else if (!known.targeterCondition().test(condition.name())) {
                    report.issues.add(new Issue(file, line, Reason.TARGETER_CONDITION, condition.name()));
                }
            }
        }

        for (String key : List.of("s", "skill", "skills")) {
            String value = mechanic.params().get(key);
            if (value == null) continue;
            if (value.trim().startsWith("[")) {
                for (String inner : SkillEngine.splitInline(value)) {
                    report.lines++;
                    SkillStep innerStep = SkillStep.parse(inner);
                    if (innerStep == null) report.issues.add(new Issue(file, inner, Reason.UNPARSEABLE, ""));
                    else checkStep(innerStep, inner, file, report);
                }
            } else if (mechanic.name().equals("skill") || mechanic.name().equals("randomskill") || mechanic.name().equals("sudoskill")) {
                for (String id : value.split(",")) {
                    if (!id.isBlank() && !known.skill().test(id.trim())) report.issues.add(new Issue(file, line, Reason.SKILL, id.trim()));
                }
            }
        }
        for (String key : List.of("os", "oe", "ot", "oh", "onstart", "onend", "ontick", "onhit")) {
            String value = mechanic.params().get(key);
            if (value == null || !value.trim().startsWith("[")) continue;
            for (String inner : SkillEngine.splitInline(value)) {
                report.lines++;
                SkillStep innerStep = SkillStep.parse(inner);
                if (innerStep == null) report.issues.add(new Issue(file, inner, Reason.UNPARSEABLE, ""));
                else checkStep(innerStep, inner, file, report);
            }
        }
    }
}
