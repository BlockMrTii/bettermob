package eu.northsoft.bettermob.api;

import org.bukkit.entity.EntityType;

/** Nur lesbare Ansicht auf eine in BetterMob registrierte Mob-Definition. */
public record MobInfo(String id, EntityType type, String displayName, String modelId, double health, double damage) {
}
