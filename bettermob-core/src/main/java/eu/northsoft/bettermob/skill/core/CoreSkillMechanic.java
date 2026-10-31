package eu.northsoft.bettermob.skill.core;

import java.util.Map;

@FunctionalInterface
public interface CoreSkillMechanic {
    void execute(CoreSkillContext context, CoreSkillTarget target, Map<String, String> params);
}
