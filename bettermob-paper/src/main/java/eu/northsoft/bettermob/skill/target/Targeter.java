package eu.northsoft.bettermob.skill.target;

import eu.northsoft.bettermob.skill.SkillContext;
import eu.northsoft.bettermob.skill.Target;

import java.util.List;
import java.util.Map;

public interface Targeter {
    List<Target> resolve(Map<String, String> params, SkillContext context);
}
