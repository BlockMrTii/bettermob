package eu.northsoft.bettermob.skill.mechanic;

import eu.northsoft.bettermob.skill.SkillContext;
import eu.northsoft.bettermob.skill.SkillStep;
import eu.northsoft.bettermob.skill.Target;

import java.util.Map;

public record MechanicCall(SkillStep.Mechanic step, SkillContext context, Target target, Map<String, String> params) {
}
