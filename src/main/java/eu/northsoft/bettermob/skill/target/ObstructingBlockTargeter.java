package eu.northsoft.bettermob.skill.target;

import eu.northsoft.bettermob.skill.SkillContext;
import eu.northsoft.bettermob.skill.Target;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.entity.LivingEntity;
import org.bukkit.util.Vector;

import java.util.Map;

public final class ObstructingBlockTargeter implements SingleTargeter {
    @Override
    public Target target(Map<String, String> params, SkillContext context) {
        return Target.ofBlock(obstructingBlock(context.caster()));
    }

    private static final double REACH = 2.5;

    private static Block obstructingBlock(LivingEntity caster) {
        Location eye = caster.getEyeLocation();
        Vector direction = eye.getDirection();
        if (!Bukkit.isOwnedByCurrentRegion(eye.clone().add(direction.clone().multiply(REACH)))) return eye.getBlock();
        var result = caster.getWorld().rayTraceBlocks(eye, direction, REACH);
        return result != null ? result.getHitBlock() : eye.add(direction).getBlock();
    }
}
