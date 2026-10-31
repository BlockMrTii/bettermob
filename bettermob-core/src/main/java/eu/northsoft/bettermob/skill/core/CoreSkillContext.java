package eu.northsoft.bettermob.skill.core;

import eu.northsoft.bettermob.platform.PlatformLivingEntity;
import eu.northsoft.bettermob.platform.Position;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public record CoreSkillContext(
        PlatformLivingEntity caster,
        PlatformLivingEntity trigger,
        Position origin,
        Map<String, String> variables,
        Map<String, String> arguments
) {
    public CoreSkillContext(PlatformLivingEntity caster) {
        this(caster, null, caster != null ? caster.position() : null, new ConcurrentHashMap<>(), Map.of());
    }

    public CoreSkillContext withTrigger(PlatformLivingEntity newTrigger) {
        return new CoreSkillContext(caster, newTrigger, origin, variables, arguments);
    }
}
