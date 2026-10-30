package eu.northsoft.bettermob.skill.target;

import eu.northsoft.bettermob.skill.Condition;
import eu.northsoft.bettermob.skill.Factions;
import eu.northsoft.bettermob.skill.SkillContext;
import eu.northsoft.bettermob.skill.SkillEngine;
import eu.northsoft.bettermob.skill.Target;
import eu.northsoft.bettermob.skill.condition.ConditionParams;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;

import java.util.List;
import java.util.Set;
import java.util.function.Predicate;

import static eu.northsoft.bettermob.skill.Params.conditionParam;
import static eu.northsoft.bettermob.skill.SkillTags.TAG_PREFIX;

public final class CandidateFilters {
    public static final Set<String> SPECIAL = Set.of("isplayer", "iscaster", "ismob", "hastag", "faction");

    public static boolean knows(String name, Predicate<String> registryHas) {
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
            if (!holds(candidate, condition, context)) return false;
        }
        return true;
    }

    private boolean holds(LivingEntity candidate, Condition condition, SkillContext context) {
        if (condition.composite()) {
            return Condition.holds(condition.params(), atom -> holds(candidate, atom, context)) == (condition.expected() == null || condition.expected());
        }
        boolean actual = switch (condition.name()) {
            case "isplayer" -> candidate instanceof Player;
            case "iscaster" -> candidate.equals(context.caster());
            case "ismob" -> !(candidate instanceof Player);
            case "hastag" -> candidate.getScoreboardTags().contains(TAG_PREFIX + conditionParam(ConditionParams.resolve(engine, condition.params(), context, Target.ofEntity(candidate)), "t", "tag", "n"));
            case "faction" -> Factions.has(engine.mobManager(), candidate, conditionParam(ConditionParams.resolve(engine, condition.params(), context, Target.ofEntity(candidate)), "faction", "f", "name"));
            default -> {
                if (!engine.conditionRegistry().has(condition.name())) {
                    engine.plugin().messages().warnOnce("skill.targeterConditionUnsupported", "condition", condition.name());
                    yield true;
                }
                SkillContext asCandidate = new SkillContext(candidate, context.caster(), null, context.origin(), false, context.variables(), context.arguments());
                yield engine.conditionRegistry().evaluate(condition, asCandidate, Target.ofEntity(candidate));
            }
        };
        return actual == (condition.expected() == null || condition.expected());
    }
}
