package eu.northsoft.bettermob.skill.target;

import eu.northsoft.bettermob.mob.MobDefinition;
import eu.northsoft.bettermob.skill.InlineSkills;
import eu.northsoft.bettermob.skill.SkillContext;
import eu.northsoft.bettermob.skill.SkillEngine;
import eu.northsoft.bettermob.skill.Target;
import org.bukkit.Bukkit;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static eu.northsoft.bettermob.skill.Params.firstParam;

public final class ModelPassengersTargeter implements Targeter {
    private final SkillEngine engine;
    private final CandidateFilters filters;

    public ModelPassengersTargeter(SkillEngine engine, CandidateFilters filters) {
        this.engine = engine;
        this.filters = filters;
    }

    @Override
    public List<Target> resolve(Map<String, String> params, SkillContext context) {
        String model = firstParam(params, "mid", "model");
        if (model != null) {
            MobDefinition definition = engine.mobManager().definitionOf(context.caster().getUniqueId());
            if (definition == null || definition.modelId == null || !definition.modelId.equalsIgnoreCase(model)) return List.of();
        }
        List<String> conditions = params.containsKey("conditions") ? InlineSkills.splitInline(params.get("conditions")) : List.of();
        List<Target> targets = new ArrayList<>();
        for (UUID id : engine.modelRiders(context.caster().getUniqueId(), firstParam(params, "p", "seat"))) {
            Entity rider = Bukkit.getEntity(id);
            if (rider instanceof LivingEntity living && Bukkit.isOwnedByCurrentRegion(living)
                    && filters.matches(living, conditions, context)) {
                targets.add(Target.ofEntity(living));
            }
        }
        return targets;
    }
}
