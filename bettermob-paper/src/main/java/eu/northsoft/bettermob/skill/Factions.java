package eu.northsoft.bettermob.skill;

import eu.northsoft.bettermob.mob.MobManager;
import org.bukkit.entity.Entity;

import java.util.Locale;

public final class Factions {
    private Factions() {}

    public static boolean has(MobManager mobManager, Entity entity, String names) {
        if (names == null) return false;
        for (String name : names.split(",")) {
            if (mobManager.inFaction(entity, name.trim().toLowerCase(Locale.ROOT))) return true;
        }
        return false;
    }
}
