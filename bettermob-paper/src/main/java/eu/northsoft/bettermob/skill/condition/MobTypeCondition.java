package eu.northsoft.bettermob.skill.condition;

import eu.northsoft.bettermob.mob.MobDefinition;
import eu.northsoft.bettermob.skill.SkillContext;
import eu.northsoft.bettermob.skill.SkillEngine;
import eu.northsoft.bettermob.skill.Target;
import org.bukkit.entity.Entity;

import static eu.northsoft.bettermob.skill.Params.conditionParam;

public final class MobTypeCondition implements SkillCondition {
    private final SkillEngine engine;

    public MobTypeCondition(SkillEngine engine) {
        this.engine = engine;
    }

    @Override
    public boolean test(SkillContext context, String paramsRaw, Target targetOverride) {
        Entity entity = targetOverride != null && targetOverride.entity() != null ? targetOverride.entity() : context.caster();
        MobDefinition definition = engine.mobManager().definitionOf(entity.getUniqueId());
        if (definition == null) return false;
        for (String id : conditionParam(paramsRaw, "t", "type", "types", "m", "mob").split(",")) {
            if (id.trim().equalsIgnoreCase(definition.id)) return true;
        }
        return false;
    }
}
