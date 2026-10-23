package eu.northsoft.bettermob.api.event.mob;

import eu.northsoft.bettermob.api.MobInfo;
import java.util.List;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Mob;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;

public class BetterMobGroupAlertEvent extends Event implements Cancellable {
    private static final HandlerList HANDLERS = new HandlerList();

    private final LivingEntity entity;
    private final MobInfo mob;
    private final LivingEntity attacker;
    private final List<Mob> members;
    private boolean cancelled;

    public BetterMobGroupAlertEvent(LivingEntity entity, MobInfo mob, LivingEntity attacker, List<Mob> members) {
        this.entity = entity;
        this.mob = mob;
        this.attacker = attacker;
        this.members = members;
    }

    public LivingEntity getEntity() {
        return entity;
    }

    public MobInfo getMob() {
        return mob;
    }

    public LivingEntity getAttacker() {
        return attacker;
    }

    public List<Mob> getMembers() {
        return members;
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
