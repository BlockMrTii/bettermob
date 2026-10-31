package eu.northsoft.bettermob.skill.mechanic;

import eu.northsoft.bettermob.skill.SkillEngine;
import org.bukkit.entity.LivingEntity;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.util.Map;

import static eu.northsoft.bettermob.skill.Params.firstParam;
import static eu.northsoft.bettermob.skill.Params.parseInt;

public final class PotionMechanic implements Mechanic {
    private final SkillEngine engine;

    public PotionMechanic(SkillEngine engine) {
        this.engine = engine;
    }

    @Override
    public void execute(MechanicCall call) {
        Map<String, String> p = call.params();
        if (!(call.target().entity() instanceof LivingEntity living)) return;
        PotionEffectType type = PotionEffectType.getByName(firstParam(p, "type", "t") == null ? "SLOW" : firstParam(p, "type", "t").trim());
        if (type == null) {
            engine.plugin().messages().warn("skill.potionUnknown", "type", p.get("type"));
            return;
        }
        int duration = parseInt(firstParam(p, "duration", "d"), 20);
        int level = parseInt(firstParam(p, "level", "l"), 1);
        living.addPotionEffect(new PotionEffect(type, duration, Math.max(0, level - 1)));
    }
}
