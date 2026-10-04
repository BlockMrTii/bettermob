package eu.northsoft.bettermob.api;

import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;

import java.util.Map;

public record ConditionContext(LivingEntity caster, LivingEntity trigger, Entity target, Location location, Map<String, String> params) {
}
