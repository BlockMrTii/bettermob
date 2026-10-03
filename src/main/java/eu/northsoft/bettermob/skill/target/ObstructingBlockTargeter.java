package eu.northsoft.bettermob.skill.target;

import eu.northsoft.bettermob.skill.SkillContext;
import eu.northsoft.bettermob.skill.Target;
import org.bukkit.block.Block;
import org.bukkit.entity.LivingEntity;

import java.util.Map;

public final class ObstructingBlockTargeter implements SingleTargeter {
    @Override
    public Target target(Map<String, String> params, SkillContext context) {
        return Target.ofBlock(obstructingBlock(context.caster()));
    }

    private static Block obstructingBlock(LivingEntity caster) {
        var result = caster.getWorld().rayTraceBlocks(caster.getEyeLocation(), caster.getEyeLocation().getDirection(), 2.5);
        return result != null ? result.getHitBlock() : caster.getEyeLocation().add(caster.getEyeLocation().getDirection()).getBlock();
    }
}
