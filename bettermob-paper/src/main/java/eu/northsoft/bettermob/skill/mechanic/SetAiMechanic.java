package eu.northsoft.bettermob.skill.mechanic;

import org.bukkit.entity.Mob;

public final class SetAiMechanic implements Mechanic {
    @Override
    public void execute(MechanicCall call) {
        if (call.target().entity() instanceof Mob mob) mob.setAI(!"false".equalsIgnoreCase(call.params().get("ai")));
    }
}
