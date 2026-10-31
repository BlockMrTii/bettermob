package eu.northsoft.bettermob.api;

import eu.northsoft.bettermob.mob.MobDefinition;
import org.bukkit.entity.EntityType;

import java.util.Locale;

public record MobInfo(String id, EntityType type, String displayName, String modelId, double health, double damage) {
    public static MobInfo from(MobDefinition definition) {
        if (definition == null) return null;
        EntityType type;
        try {
            type = EntityType.valueOf(definition.type.toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            type = EntityType.ZOMBIE;
        }
        return new MobInfo(definition.id, type, definition.displayName, definition.modelId, definition.health, definition.damage);
    }
}
