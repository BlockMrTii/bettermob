package eu.northsoft.bettermob.skill.mechanic;

import org.bukkit.attribute.Attribute;
import org.bukkit.entity.LivingEntity;

import static eu.northsoft.bettermob.skill.Params.firstParam;
import static eu.northsoft.bettermob.skill.Params.parseFloat;

public final class SetSpeedMechanic implements Mechanic {
    @Override
    public void execute(MechanicCall call) {
        if (!(call.target().entity() instanceof LivingEntity living)) return;
        var speed = living.getAttribute(Attribute.MOVEMENT_SPEED);
        if (speed != null) speed.setBaseValue(Math.max(0, parseFloat(firstParam(call.params(), "speed", "s"), (float) speed.getBaseValue())));
    }
}
