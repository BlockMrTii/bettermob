package eu.northsoft.bettermob.api.event.mob;

import eu.northsoft.bettermob.api.MobInfo;
import org.bukkit.entity.LivingEntity;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;

public class BetterMobKillEvent extends Event {
    private static final HandlerList HANDLERS = new HandlerList();

    private final LivingEntity entity;
    private final MobInfo mob;
    private final LivingEntity victim;

    public BetterMobKillEvent(LivingEntity entity, MobInfo mob, LivingEntity victim) {
        this.entity = entity;
        this.mob = mob;
        this.victim = victim;
    }

    public LivingEntity getEntity() {
        return entity;
    }

    public MobInfo getMob() {
        return mob;
    }

    public LivingEntity getVictim() {
        return victim;
    }

    @NotNull
    @Override
    public HandlerList getHandlers() {
        return HANDLERS;
    }

    @NotNull
    public static HandlerList getHandlerList() {
        return HANDLERS;
    }
}
