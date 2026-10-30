package eu.northsoft.bettermob.skill.mechanic;

import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import static eu.northsoft.bettermob.skill.Params.firstParam;
import static eu.northsoft.bettermob.skill.Params.parseInt;

public final class BlackScreenMechanic implements Mechanic {
    private static final int MAX_TICKS = 20 * 60;

    @Override
    public void execute(MechanicCall call) {
        if (!(call.target().entity() instanceof Player player)) return;
        int ticks = Math.max(1, Math.min(MAX_TICKS, parseInt(firstParam(call.params(), "d", "duration"), 20)));
        player.addPotionEffect(new PotionEffect(PotionEffectType.BLINDNESS, ticks, 0, false, false, false));
        player.addPotionEffect(new PotionEffect(PotionEffectType.DARKNESS, ticks, 0, false, false, false));
    }
}
