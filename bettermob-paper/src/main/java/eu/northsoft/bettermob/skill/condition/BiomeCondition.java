package eu.northsoft.bettermob.skill.condition;

import eu.northsoft.bettermob.skill.SkillContext;
import eu.northsoft.bettermob.skill.Target;
import org.bukkit.Location;

import java.util.Locale;

import static eu.northsoft.bettermob.skill.Params.conditionParam;

public final class BiomeCondition implements SkillCondition {
    @Override
    public boolean test(SkillContext context, String paramsRaw, Target targetOverride) {
        Location at = context.caster().getLocation();
        String biome = at.getWorld().getBiome(at).getKey().getKey();
        for (String candidate : conditionParam(paramsRaw, "b", "biome").split(",")) {
            String name = candidate.trim().toLowerCase(Locale.ROOT);
            if (name.startsWith("minecraft:")) name = name.substring("minecraft:".length());
            if (name.equals(biome)) return true;
        }
        return false;
    }
}
