package eu.northsoft.bettermob.api.event;

import eu.northsoft.bettermob.api.MobInfo;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;
import org.jetbrains.annotations.NotNull;

public class BetterMobDamageEvent extends Event implements Cancellable {
    private static final HandlerList HANDLERS = new HandlerList();

    private final LivingEntity entity;
    private final MobInfo mob;
    private final Entity other;
    private final boolean mobIsVictim;
    private final DamageCause cause;
    private double damage;
    private boolean cancelled;

    public BetterMobDamageEvent(LivingEntity entity, MobInfo mob, Entity other, boolean mobIsVictim, DamageCause cause, double damage) {
        this.entity = entity;
        this.mob = mob;
        this.other = other;
        this.mobIsVictim = mobIsVictim;
        this.cause = cause;
        this.damage = damage;
    }

    public LivingEntity getEntity() {
        return entity;
    }

    public MobInfo getMob() {
        return mob;
    }

    public Entity getOther() {
        return other;
    }

    public boolean isMobVictim() {
        return mobIsVictim;
    }

    public DamageCause getCause() {
        return cause;
    }

    public double getDamage() {
        return damage;
    }

    public void setDamage(double damage) {
        this.damage = Math.max(0, damage);
    }

    @Override
    public boolean isCancelled() {
        return cancelled;
    }

    @Override
    public void setCancelled(boolean cancelled) {
        this.cancelled = cancelled;
    }

    @Override
    public @NotNull HandlerList getHandlers() {
        return HANDLERS;
    }

    public static HandlerList getHandlerList() {
        return HANDLERS;
    }
}
