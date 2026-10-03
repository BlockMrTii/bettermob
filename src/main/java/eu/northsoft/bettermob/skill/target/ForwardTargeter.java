package eu.northsoft.bettermob.skill.target;

import eu.northsoft.bettermob.skill.SkillContext;
import eu.northsoft.bettermob.skill.Target;
import org.bukkit.Location;
import org.bukkit.entity.LivingEntity;
import org.bukkit.util.Vector;

import java.util.Map;

import static eu.northsoft.bettermob.skill.Params.parseFloat;

public final class ForwardTargeter implements SingleTargeter {
    @Override
    public Target target(Map<String, String> params, SkillContext context) {
        return Target.ofLocation(forwardLocation(context.caster(), params));
    }

    private static Location forwardLocation(LivingEntity caster, Map<String, String> p) {
        Location origin = Boolean.parseBoolean(p.getOrDefault("uel", "false")) ? caster.getEyeLocation() : caster.getLocation();
        double distance = parseFloat(p.get("f"), 1f);
        double yOffset = parseFloat(p.get("yoffset"), 0f);
        double rotate = parseFloat(p.get("rotate"), 0f);
        Vector direction = origin.getDirection().normalize();

        if (rotate != 0) direction.rotateAroundY(Math.toRadians(-rotate));
        Location target = origin.clone().add(direction.multiply(distance));
        target.add(0, yOffset, 0);
        return target;
    }
}
