package eu.northsoft.bettermob.skill.mechanic;

import org.bukkit.entity.LivingEntity;

import static eu.northsoft.bettermob.skill.Params.firstParam;
import static eu.northsoft.bettermob.skill.Params.parseInt;

public final class SetNoDamageTicksMechanic implements Mechanic {
    @Override
    public void execute(MechanicCall call) {
        if (call.target().entity() instanceof LivingEntity living) living.setNoDamageTicks(parseInt(firstParam(call.params(), "ticks", "t"), 0));
    }
}
