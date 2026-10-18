package eu.northsoft.bettermob.skill.condition;

import eu.northsoft.bettermob.skill.SkillContext;
import eu.northsoft.bettermob.skill.SkillEngine;
import eu.northsoft.bettermob.skill.Target;
import org.bukkit.entity.Player;

import static eu.northsoft.bettermob.skill.Params.conditionParam;

public final class KillCondition implements SkillCondition {
    private final SkillEngine engine;
    private final boolean total;

    public KillCondition(SkillEngine engine, boolean total) {
        this.engine = engine;
        this.total = total;
    }

    static Player playerOf(SkillContext context, Target targetOverride) {
        if (targetOverride != null && targetOverride.entity() instanceof Player player) return player;
        if (context.trigger() instanceof Player player) return player;
        return context.caster() instanceof Player player ? player : null;
    }

    static String mobParam(String paramsRaw) {
        return conditionParam(paramsRaw, "mob", "id", "m");
    }

    @Override
    public boolean test(SkillContext context, String paramsRaw, Target targetOverride) {
        Player player = playerOf(context, targetOverride);
        if (player == null || engine.plugin().kills() == null) return false;
        String mob = mobParam(paramsRaw);
        if (!total && mob.isEmpty()) return false;
        int kills = total || mob.isEmpty() ? engine.plugin().kills().total(player.getUniqueId()) : engine.plugin().kills().count(player.getUniqueId(), mob);
        String count = conditionParam(paramsRaw, "count", "amount", "a", "value");
        return RangeSpec.matches(RangeSpec.atLeast(count.isEmpty() ? "1" : count), kills);
    }
}
