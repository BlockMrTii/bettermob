package eu.northsoft.bettermob.api.custom;

import org.bukkit.entity.LivingEntity;

public record PlaceholderContext(LivingEntity caster, LivingEntity trigger) {
}
