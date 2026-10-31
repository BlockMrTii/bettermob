package eu.northsoft.bettermob.skill.core;

import eu.northsoft.bettermob.platform.PlatformEntity;
import eu.northsoft.bettermob.platform.Position;

public record CoreSkillTarget(PlatformEntity entity, Position position) {
    public static CoreSkillTarget ofEntity(PlatformEntity entity) {
        return new CoreSkillTarget(entity, entity != null ? entity.position() : null);
    }

    public static CoreSkillTarget ofPosition(Position position) {
        return new CoreSkillTarget(null, position);
    }
}
