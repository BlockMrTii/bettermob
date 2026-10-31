package eu.northsoft.bettermob.api.event.mob;

import eu.northsoft.bettermob.api.MobInfo;
import org.bukkit.entity.LivingEntity;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;

public class BetterMobSignalEvent extends Event implements Cancellable {
    private static final HandlerList HANDLERS = new HandlerList();

    private final LivingEntity entity;
    private final MobInfo mob;
    private final String signal;
    private final LivingEntity sender;
    private boolean cancelled;

    public BetterMobSignalEvent(LivingEntity entity, MobInfo mob, String signal, LivingEntity sender) {
        this.entity = entity;
        this.mob = mob;
        this.signal = signal;
        this.sender = sender;
    }

    public LivingEntity getEntity() {
        return entity;
    }

    public MobInfo getMob() {
        return mob;
    }

    public String getSignal() {
        return signal;
    }

    public LivingEntity getSender() {
        return sender;
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
