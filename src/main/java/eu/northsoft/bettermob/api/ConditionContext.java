package eu.northsoft.bettermob.api;

import org.bukkit.entity.LivingEntity;

import java.util.Map;

public record ConditionContext(LivingEntity caster, LivingEntity trigger, Map<String, String> params) {
}
