package eu.northsoft.bettermob.skill.condition;

import eu.northsoft.bettermob.skill.SkillContext;
import eu.northsoft.bettermob.skill.Target;
import eu.northsoft.bettermob.util.RegionEntities;
import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;

import static eu.northsoft.bettermob.skill.Params.conditionParam;
import static eu.northsoft.bettermob.skill.Params.parseFloat;

public final class PlayerWithinCondition implements SkillCondition {
    @Override
    public boolean test(SkillContext context, String paramsRaw, Target targetOverride) {
        double distance = Math.max(0, parseFloat(conditionParam(paramsRaw, "d", "distance", "r", "radius"), 10f));
        Location at = context.caster().getLocation();
        int found = 0;
        for (Entity entity : RegionEntities.near(at, distance)) {
            if (entity instanceof Player player && !player.isDead() && player.getLocation().distanceSquared(at) <= distance * distance) found++;
        }
        String count = conditionParam(paramsRaw, "count", "amount", "a");
        return count.isEmpty() ? found > 0 : RangeSpec.matches(RangeSpec.atLeast(count), found);
    }
}
