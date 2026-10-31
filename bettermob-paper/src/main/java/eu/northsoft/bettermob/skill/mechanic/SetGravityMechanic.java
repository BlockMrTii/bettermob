package eu.northsoft.bettermob.skill.mechanic;

import org.bukkit.entity.Entity;

import static eu.northsoft.bettermob.skill.Params.firstParam;
import static eu.northsoft.bettermob.skill.Params.stripQuotes;

public final class SetGravityMechanic implements Mechanic {
    @Override
    public void execute(MechanicCall call) {
        Entity entity = call.target().entity();
        String value = firstParam(call.params(), "g", "gravity", "value");
        if (entity == null) return;
        entity.setGravity(value == null || !"false".equalsIgnoreCase(stripQuotes(value)));
    }
}
