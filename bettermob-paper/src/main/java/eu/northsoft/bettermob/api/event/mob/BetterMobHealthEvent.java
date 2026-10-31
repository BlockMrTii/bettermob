package eu.northsoft.bettermob.api.event.mob;

import eu.northsoft.bettermob.api.MobInfo;
import org.bukkit.entity.LivingEntity;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;

public class BetterMobHealthEvent extends Event {
    private static final HandlerList HANDLERS = new HandlerList();

    private final LivingEntity entity;
    private final MobInfo mob;
    private final double before;
    private final double after;
    private final String threshold;

    public BetterMobHealthEvent(LivingEntity entity, MobInfo mob, double before, double after, String threshold) {
        this.entity = entity;
        this.mob = mob;
        this.before = before;
        this.after = after;
        this.threshold = threshold;
    }

    public LivingEntity getEntity() {
        return entity;
    }

    public MobInfo getMob() {
        return mob;
    }

    public double getBefore() {
        return before;
    }

    public double getAfter() {
        return after;
    }

    public String getThreshold() {
        return threshold;
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
