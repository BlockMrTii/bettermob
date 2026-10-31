package eu.northsoft.bettermob.skill.target;

import eu.northsoft.bettermob.skill.SkillContext;
import eu.northsoft.bettermob.skill.Target;

import java.util.List;
import java.util.Map;

public interface SingleTargeter extends Targeter {
    Target target(Map<String, String> params, SkillContext context);

    @Override
    default List<Target> resolve(Map<String, String> params, SkillContext context) {
        Target target = target(params, context);
        return target == null ? List.of() : List.of(target);
    }
}
