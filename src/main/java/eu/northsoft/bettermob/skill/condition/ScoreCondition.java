package eu.northsoft.bettermob.skill.condition;

import eu.northsoft.bettermob.skill.SkillContext;
import eu.northsoft.bettermob.skill.SkillEngine;
import eu.northsoft.bettermob.skill.Target;
import org.bukkit.Bukkit;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.scoreboard.Objective;
import org.bukkit.scoreboard.Score;
import org.bukkit.scoreboard.ScoreboardManager;

import java.util.Map;

import static eu.northsoft.bettermob.skill.Params.conditionParam;

public final class ScoreCondition implements SkillCondition {
    private final SkillEngine engine;

    public ScoreCondition(SkillEngine engine) {
        this.engine = engine;
    }

    @Override
    public boolean test(SkillContext context, String paramsRaw, Target targetOverride) {
        Entity entity = targetOverride != null && targetOverride.entity() != null ? targetOverride.entity()
                : context.trigger() != null ? context.trigger() : context.caster();
        Map<String, String> caster = engine.state().existingVariablesOf(context.caster().getUniqueId());
        Map<String, String> target = engine.state().existingVariablesOf(entity.getUniqueId());
        String objectiveName = SkillEngine.replaceVariables(conditionParam(paramsRaw, "o", "objective", "obj"), context.variables(), caster, target);
        String value = SkillEngine.replaceVariables(conditionParam(paramsRaw, "v", "value", "val"), context.variables(), caster, target);
        if (objectiveName.isEmpty() || value.isEmpty()) return false;
        try {
            ScoreboardManager manager = Bukkit.getScoreboardManager();
            Objective objective = manager == null ? null : manager.getMainScoreboard().getObjective(objectiveName);
            if (objective == null) return false;
            Score score = objective.getScore(entity instanceof Player player ? player.getName() : entity.getUniqueId().toString());
            return score.isScoreSet() && RangeSpec.matches(value, score.getScore());
        } catch (UnsupportedOperationException exception) {
            return false;
        }
    }
}
