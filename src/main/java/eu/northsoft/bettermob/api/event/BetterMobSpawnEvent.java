package eu.northsoft.bettermob.api.event;

import eu.northsoft.bettermob.api.MobInfo;
import org.bukkit.entity.LivingEntity;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;

/** Wird ausgeloest, nachdem BetterMob einen Mob gespawnt hat (nach Attributen, Modell und ~onSpawn-Skills). */
public class BetterMobSpawnEvent extends Event {
    private static final HandlerList HANDLERS = new HandlerList();

    private final LivingEntity entity;
    private final MobInfo mob;

    public BetterMobSpawnEvent(LivingEntity entity, MobInfo mob) {
        this.entity = entity;
        this.mob = mob;
    }

    public LivingEntity getEntity() {
        return entity;
    }

    public MobInfo getMob() {
        return mob;
    }

    @Override
    public @NotNull HandlerList getHandlers() {
        return HANDLERS;
    }

    public static HandlerList getHandlerList() {
        return HANDLERS;
    }
}
