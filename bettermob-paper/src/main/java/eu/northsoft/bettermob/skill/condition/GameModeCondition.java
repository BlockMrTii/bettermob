package eu.northsoft.bettermob.skill.condition;

import eu.northsoft.bettermob.skill.SkillContext;
import eu.northsoft.bettermob.skill.Target;
import org.bukkit.entity.Player;

import java.util.Locale;

import static eu.northsoft.bettermob.skill.Params.conditionParam;

public final class GameModeCondition implements SkillCondition {
    @Override
    public boolean test(SkillContext context, String paramsRaw, Target targetOverride) {
        if (!(context.caster() instanceof Player player)) return false;
        String wanted = conditionParam(paramsRaw, "gm", "mode", "gamemode", "m").toUpperCase(Locale.ROOT);
        for (String name : wanted.split(",")) {
            if (player.getGameMode().name().equals(name.trim())) return true;
        }
        return false;
    }
}
