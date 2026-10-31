package eu.northsoft.bettermob.skill.condition;

import eu.northsoft.bettermob.skill.SkillContext;
import eu.northsoft.bettermob.skill.Target;

import java.util.Locale;

import static eu.northsoft.bettermob.skill.Params.conditionParam;

public final class TimeCondition implements SkillCondition {
    @Override
    public boolean test(SkillContext context, String paramsRaw, Target targetOverride) {
        long time = context.caster().getWorld().getTime();
        String spec = conditionParam(paramsRaw, "t", "time").toLowerCase(Locale.ROOT);
        return switch (spec) {
            case "day" -> !isNight(time);
            case "night" -> isNight(time);
            default -> RangeSpec.matches(spec, time);
        };
    }

    static boolean isNight(long time) {
        return time >= 13000 && time < 23000;
    }
}
