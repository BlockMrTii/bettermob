package eu.northsoft.bettermob.skill.mechanic;

import org.bukkit.attribute.Attribute;
import org.bukkit.entity.LivingEntity;

import static eu.northsoft.bettermob.skill.Params.firstParam;
import static eu.northsoft.bettermob.skill.Params.parseFloat;

public final class HealMechanic implements Mechanic {
    @Override
    public void execute(MechanicCall call) {
        if (!(call.target().entity() instanceof LivingEntity living) || living.isDead()) return;
        double amount = parseFloat(firstParam(call.params(), "amount", "a"), 1f);
        var maxHealth = living.getAttribute(Attribute.MAX_HEALTH);
        double max = maxHealth == null ? living.getHealth() : maxHealth.getValue();
        living.setHealth(Math.max(0, Math.min(max, living.getHealth() + amount)));
    }
}
