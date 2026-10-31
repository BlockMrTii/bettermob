package eu.northsoft.bettermob.skill.core;

@FunctionalInterface
public interface CoreSkillCondition {
    boolean test(CoreSkillContext context, String paramsRaw, CoreSkillTarget target);
}
