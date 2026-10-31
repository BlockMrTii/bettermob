package eu.northsoft.bettermob.api.event.mob;

import eu.northsoft.bettermob.api.MobInfo;
import org.bukkit.entity.LivingEntity;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;

public class BetterMobPhaseEvent extends Event {
    private static final HandlerList HANDLERS = new HandlerList();

    private final LivingEntity entity;
    private final MobInfo mob;
    private final int from;
    private final int to;

    public BetterMobPhaseEvent(LivingEntity entity, MobInfo mob, int from, int to) {
        this.entity = entity;
        this.mob = mob;
        this.from = from;
        this.to = to;
    }

    public LivingEntity getEntity() {
        return entity;
    }

    public MobInfo getMob() {
        return mob;
    }

    public int getFrom() {
        return from;
    }

    public int getTo() {
        return to;
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
