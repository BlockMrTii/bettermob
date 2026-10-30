package eu.northsoft.bettermob.skill.condition;

import eu.northsoft.bettermob.mob.MobDefinition;
import eu.northsoft.bettermob.skill.SkillContext;
import eu.northsoft.bettermob.skill.SkillEngine;
import eu.northsoft.bettermob.skill.Target;
import org.bukkit.entity.Entity;

import java.util.UUID;

import static eu.northsoft.bettermob.skill.Params.conditionParam;

public final class ModelRideCondition implements SkillCondition {
    public enum Kind { HAS_PASSENGERS, RIDING_MODEL, MOUNTED }

    private final SkillEngine engine;
    private final Kind kind;

    public ModelRideCondition(SkillEngine engine, Kind kind) {
        this.engine = engine;
        this.kind = kind;
    }

    @Override
    public boolean test(SkillContext context, String paramsRaw, Target targetOverride) {
        Entity entity = targetOverride != null && targetOverride.entity() != null ? targetOverride.entity() : context.caster();
        return switch (kind) {
            case HAS_PASSENGERS -> !engine.modelRiders(entity.getUniqueId(), conditionParam(paramsRaw, "p", "seat")).isEmpty();
            case MOUNTED -> engine.modelOwnerOf(entity.getUniqueId()) != null || entity.getVehicle() != null;
            case RIDING_MODEL -> {
                UUID owner = engine.modelOwnerOf(entity.getUniqueId());
                if (owner == null) yield false;
                String model = conditionParam(paramsRaw, "mid", "model");
                if (model.isEmpty()) yield true;
                MobDefinition definition = engine.mobManager().definitionOf(owner);
                yield definition != null && definition.modelId != null && definition.modelId.equalsIgnoreCase(model);
            }
        };
    }
}
