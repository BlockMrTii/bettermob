package eu.northsoft.bettermob.skill.target;

import eu.northsoft.bettermob.skill.SkillContext;
import eu.northsoft.bettermob.skill.SkillEngine;
import eu.northsoft.bettermob.skill.Target;
import org.bukkit.Bukkit;
import org.bukkit.entity.Entity;

import java.util.Map;
import java.util.UUID;

public final class MountedModelTargeter implements SingleTargeter {
    private final SkillEngine engine;

    public MountedModelTargeter(SkillEngine engine) {
        this.engine = engine;
    }

    @Override
    public Target target(Map<String, String> params, SkillContext context) {
        UUID owner = engine.modelPassengers().ownerOf(context.caster().getUniqueId());
        if (owner == null) return null;
        Entity entity = Bukkit.getEntity(owner);
        return entity == null ? null : Target.ofEntity(entity);
    }
}
