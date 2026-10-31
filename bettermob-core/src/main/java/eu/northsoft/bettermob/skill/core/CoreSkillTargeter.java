package eu.northsoft.bettermob.skill.core;

import java.util.List;
import java.util.Map;

@FunctionalInterface
public interface CoreSkillTargeter {
    List<CoreSkillTarget> resolve(Map<String, String> params, CoreSkillContext context);
}
