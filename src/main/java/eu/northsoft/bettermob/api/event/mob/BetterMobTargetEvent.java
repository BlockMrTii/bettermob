package eu.northsoft.bettermob.api.event.mob;

import eu.northsoft.bettermob.api.MobInfo;
import org.bukkit.entity.LivingEntity;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;

public class BetterMobTargetEvent extends Event implements Cancellable {
    private static final HandlerList HANDLERS = new HandlerList();

    private final LivingEntity entity;
    private final MobInfo mob;
    private final LivingEntity oldTarget;
    private final LivingEntity newTarget;
    private boolean cancelled;

    public BetterMobTargetEvent(LivingEntity entity, MobInfo mob, LivingEntity oldTarget, LivingEntity newTarget) {
        this.entity = entity;
        this.mob = mob;
        this.oldTarget = oldTarget;
        this.newTarget = newTarget;
    }

    public LivingEntity getEntity() {
        return entity;
    }

    public MobInfo getMob() {
        return mob;
    }

    public LivingEntity getOldTarget() {
        return oldTarget;
    }

    public LivingEntity getNewTarget() {
        return newTarget;
    }

    @Override
    public boolean isCancelled() {
        return cancelled;
    }

    @Override
    public void setCancelled(boolean cancel) {
        this.cancelled = cancel;
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
