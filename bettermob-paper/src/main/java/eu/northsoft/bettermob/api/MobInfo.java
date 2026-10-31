package eu.northsoft.bettermob.api;

import org.bukkit.entity.EntityType;

public record MobInfo(String id, EntityType type, String displayName, String modelId, double health, double damage) {
}
