package eu.northsoft.bettermob.skill.target;

import eu.northsoft.bettermob.skill.SkillContext;
import eu.northsoft.bettermob.skill.Target;

import java.util.Map;

import static eu.northsoft.bettermob.skill.Params.parseFloat;

public final class SelfLocationTargeter implements SingleTargeter {
    @Override
    public Target target(Map<String, String> params, SkillContext context) {
        return Target.ofLocation(context.caster().getLocation().add(
                parseFloat(params.get("x"), 0f), parseFloat(params.get("y"), 0f), parseFloat(params.get("z"), 0f)));
    }
}
