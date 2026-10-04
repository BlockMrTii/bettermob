package eu.northsoft.bettermob.api;

import org.bukkit.entity.LivingEntity;

public record PlaceholderContext(LivingEntity caster, LivingEntity trigger) {
}
