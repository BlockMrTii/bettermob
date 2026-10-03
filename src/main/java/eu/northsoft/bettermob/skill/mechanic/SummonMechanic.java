package eu.northsoft.bettermob.skill.mechanic;

import eu.northsoft.bettermob.mob.MobDefinition;
import eu.northsoft.bettermob.skill.SkillEngine;
import org.bukkit.entity.LivingEntity;

import static eu.northsoft.bettermob.skill.Params.firstParam;
import static eu.northsoft.bettermob.skill.SkillTags.OWNER_PREFIX;

public final class SummonMechanic implements Mechanic {
    private final SkillEngine engine;

    public SummonMechanic(SkillEngine engine) {
        this.engine = engine;
    }

    @Override
    public void execute(MechanicCall call) {
        String mobId = firstParam(call.params(), "type", "t", "mob", "m");
        if (mobId == null) return;
        MobDefinition definition = engine.mobManager().registry().get(mobId.trim());
        if (definition == null) {
            engine.plugin().messages().warn("skill.summonNotRegistered", "mob", mobId);
            return;
        }
        LivingEntity spawned = engine.mobManager().spawn(definition, call.target().location());
        if (spawned != null) spawned.addScoreboardTag(OWNER_PREFIX + call.context().caster().getUniqueId());
    }
}
