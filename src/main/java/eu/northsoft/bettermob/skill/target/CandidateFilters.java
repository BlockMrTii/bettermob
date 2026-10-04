package eu.northsoft.bettermob.skill.target;

import eu.northsoft.bettermob.skill.Condition;
import eu.northsoft.bettermob.skill.Factions;
import eu.northsoft.bettermob.skill.SkillContext;
import eu.northsoft.bettermob.skill.SkillEngine;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;

import java.util.List;

import static eu.northsoft.bettermob.skill.Params.conditionParam;
import static eu.northsoft.bettermob.skill.SkillTags.TAG_PREFIX;

public final class CandidateFilters {
    public static final java.util.Set<String> SPECIAL = java.util.Set.of("isplayer", "iscaster", "ismob", "hastag", "faction");

    public static boolean knows(String name, java.util.function.Predicate<String> registryHas) {
        return SPECIAL.contains(name) || registryHas.test(name);
    }

    private final SkillEngine engine;

    public CandidateFilters(SkillEngine engine) {
        this.engine = engine;
    }

    public boolean matches(LivingEntity candidate, List<String> conditions, SkillContext context) {
        for (String raw : conditions) {
            Condition condition = Condition.parse(raw);
            if (condition == null) continue;
            boolean actual = switch (condition.name()) {
                case "isplayer" -> candidate instanceof Player;
                case "iscaster" -> candidate.equals(context.caster());
                case "ismob" -> !(candidate instanceof Player);
                case "hastag" -> candidate.getScoreboardTags().contains(TAG_PREFIX + conditionParam(condition.params(), "t", "tag", "n"));
                case "faction" -> Factions.has(engine.mobManager(), candidate, conditionParam(condition.params(), "faction", "f", "name"));
                default -> {
                    if (!engine.conditionRegistry().has(condition.name())) {
                        engine.plugin().messages().warn("skill.targeterConditionUnsupported", "condition", condition.name());
                        yield true;
                    }
                    SkillContext asCandidate = new SkillContext(candidate, context.caster(), null, context.origin(), false);
                    yield engine.conditionRegistry().evaluate(condition, asCandidate, null);
                }
            };
            if (actual != (condition.expected() == null || condition.expected())) return false;
        }
        return true;
    }
}
