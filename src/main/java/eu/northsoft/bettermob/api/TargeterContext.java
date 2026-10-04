package eu.northsoft.bettermob.api;

import org.bukkit.Location;
import org.bukkit.entity.LivingEntity;

import java.util.Map;

public record TargeterContext(LivingEntity caster, LivingEntity trigger, Location origin, Map<String, String> params) {
}
