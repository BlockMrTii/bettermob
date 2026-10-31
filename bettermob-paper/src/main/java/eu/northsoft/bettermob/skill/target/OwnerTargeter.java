package eu.northsoft.bettermob.skill.target;

import eu.northsoft.bettermob.skill.SkillContext;
import eu.northsoft.bettermob.skill.Target;
import org.bukkit.Bukkit;
import org.bukkit.entity.Entity;

import java.util.Map;
import java.util.UUID;

import static eu.northsoft.bettermob.skill.SkillTags.OWNER_PREFIX;

public final class OwnerTargeter implements SingleTargeter {
    @Override
    public Target target(Map<String, String> params, SkillContext context) {
        for (String tag : context.caster().getScoreboardTags()) {
            if (!tag.startsWith(OWNER_PREFIX)) continue;
            try {
                Entity owner = Bukkit.getEntity(UUID.fromString(tag.substring(OWNER_PREFIX.length())));
                if (owner != null) return Target.ofEntity(owner);
            } catch (IllegalArgumentException exception) {
                return null;
            }
        }
        return null;
    }
}
