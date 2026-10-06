package eu.northsoft.bettermob.example;

import eu.northsoft.bettermob.api.event.BetterMobDamageEvent;
import eu.northsoft.bettermob.api.event.BetterMobSkillEvent;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;

public final class ModernEvents implements Listener {
    @EventHandler
    public void onDamage(BetterMobDamageEvent event) {
        if (event.isMobVictim() && event.getOther() instanceof Player) event.setDamage(event.getDamage() * 0.5);
    }

    @EventHandler
    public void onSkill(BetterMobSkillEvent event) {
        if (event.getSkillId().equals("example_blocked")) event.setCancelled(true);
    }
}
