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
    public static final java.util.Set<String> KNOWN = java.util.Set.of("isplayer", "iscaster", "ismob", "hastag", "faction");

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
                    engine.plugin().messages().warn("skill.targeterConditionUnsupported", "condition", condition.name());
                    yield true;
                }
            };
            if (actual != (condition.expected() == null || condition.expected())) return false;
        }
        return true;
    }
}
