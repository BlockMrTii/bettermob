package eu.northsoft.bettermob.skill.target;

import eu.northsoft.bettermob.skill.SkillContext;
import eu.northsoft.bettermob.skill.Target;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;

import java.util.Map;

import static eu.northsoft.bettermob.skill.Params.firstParam;
import static eu.northsoft.bettermob.skill.Params.parseFloat;

public final class FixedLocationTargeter implements SingleTargeter {
    @Override
    public Target target(Map<String, String> p, SkillContext context) {
        World world = context.caster().getWorld();
        String name = firstParam(p, "w", "world");
        if (name != null && Bukkit.getWorld(name.trim()) != null) world = Bukkit.getWorld(name.trim());
        return Target.ofLocation(new Location(world, parseFloat(p.get("x"), 0f), parseFloat(p.get("y"), 0f), parseFloat(p.get("z"), 0f)));
    }
}
