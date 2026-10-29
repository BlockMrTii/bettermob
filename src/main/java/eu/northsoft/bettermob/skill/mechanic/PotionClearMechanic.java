package eu.northsoft.bettermob.skill.mechanic;

import org.bukkit.entity.LivingEntity;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import static eu.northsoft.bettermob.skill.Params.firstParam;

public final class PotionClearMechanic implements Mechanic {
    @Override
    public void execute(MechanicCall call) {
        if (!(call.target().entity() instanceof LivingEntity living)) return;
        String types = firstParam(call.params(), "type", "t", "types");
        if (types == null || types.isBlank()) {
            for (PotionEffect effect : living.getActivePotionEffects()) living.removePotionEffect(effect.getType());
            return;
        }
        for (String name : types.split(",")) {
            PotionEffectType type = PotionEffectType.getByName(name.trim());
            if (type != null) living.removePotionEffect(type);
        }
    }
}
