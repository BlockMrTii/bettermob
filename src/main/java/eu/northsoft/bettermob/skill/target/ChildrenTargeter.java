package eu.northsoft.bettermob.skill.target;

import eu.northsoft.bettermob.skill.SkillContext;
import eu.northsoft.bettermob.skill.SkillTags;
import eu.northsoft.bettermob.skill.Target;
import eu.northsoft.bettermob.util.RegionEntities;
import org.bukkit.entity.Entity;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import static eu.northsoft.bettermob.skill.Params.firstParam;
import static eu.northsoft.bettermob.skill.Params.parseFloat;

public final class ChildrenTargeter implements Targeter {
    private static final float DEFAULT_RADIUS = 32;

    @Override
    public List<Target> resolve(Map<String, String> params, SkillContext context) {
        String tag = SkillTags.OWNER_PREFIX + context.caster().getUniqueId();
        String type = firstParam(params, "target", "type", "t");
        double radius = Math.max(1, parseFloat(firstParam(params, "r", "radius"), DEFAULT_RADIUS));
        List<Target> targets = new ArrayList<>();
        for (Entity entity : RegionEntities.near(context.caster().getLocation(), radius)) {
            if (!entity.getScoreboardTags().contains(tag)) continue;
            if (type != null && !entity.getType().name().equalsIgnoreCase(type.trim().toUpperCase(Locale.ROOT))) continue;
            targets.add(Target.ofEntity(entity));
        }
        return targets;
    }
}
