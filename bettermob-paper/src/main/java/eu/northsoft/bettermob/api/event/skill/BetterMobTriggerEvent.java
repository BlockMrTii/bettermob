package eu.northsoft.bettermob.api.event.skill;

import eu.northsoft.bettermob.api.MobInfo;
import org.bukkit.entity.LivingEntity;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;

public class BetterMobTriggerEvent extends Event implements Cancellable {
    private static final HandlerList HANDLERS = new HandlerList();

    private final LivingEntity entity;
    private final MobInfo mob;
    private final String trigger;
    private final LivingEntity source;
    private boolean cancelled;

    public BetterMobTriggerEvent(LivingEntity entity, MobInfo mob, String trigger, LivingEntity source) {
        this.entity = entity;
        this.mob = mob;
        this.trigger = trigger;
        this.source = source;
    }

    public LivingEntity getEntity() {
        return entity;
    }

    public MobInfo getMob() {
        return mob;
    }

    public String getTrigger() {
        return trigger;
    }

    public LivingEntity getSource() {
        return source;
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
