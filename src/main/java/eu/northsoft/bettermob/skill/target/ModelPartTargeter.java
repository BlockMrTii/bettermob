package eu.northsoft.bettermob.skill.target;

import eu.northsoft.bettermob.skill.SkillContext;
import eu.northsoft.bettermob.skill.SkillEngine;
import eu.northsoft.bettermob.skill.Target;
import org.bukkit.Location;

import java.util.Map;

import static eu.northsoft.bettermob.skill.Params.firstParam;

public final class ModelPartTargeter implements SingleTargeter {
    private final SkillEngine engine;

    public ModelPartTargeter(SkillEngine engine) {
        this.engine = engine;
    }

    @Override
    public Target target(Map<String, String> params, SkillContext context) {
        Location bone = engine.betterModel().bonePosition(engine.mobManager().trackerFor(context.caster().getUniqueId()),
                firstParam(params, "p", "part", "bone"), context.caster().getLocation());
        return Target.ofLocation(bone != null ? bone
                : context.caster().getLocation().add(0, context.caster().getHeight() * 0.6, 0));
    }
}
