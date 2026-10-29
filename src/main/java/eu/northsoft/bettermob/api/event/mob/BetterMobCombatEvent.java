package eu.northsoft.bettermob.api.event.mob;

import eu.northsoft.bettermob.api.MobInfo;
import org.bukkit.entity.LivingEntity;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;

public class BetterMobCombatEvent extends Event {
    private static final HandlerList HANDLERS = new HandlerList();

    private final LivingEntity entity;
    private final MobInfo mob;
    private final boolean entering;
    private final LivingEntity target;

    public BetterMobCombatEvent(LivingEntity entity, MobInfo mob, boolean entering, LivingEntity target) {
        this.entity = entity;
        this.mob = mob;
        this.entering = entering;
        this.target = target;
    }

    public LivingEntity getEntity() {
        return entity;
    }

    public MobInfo getMob() {
        return mob;
    }

    public boolean isEntering() {
        return entering;
    }

    public LivingEntity getTarget() {
        return target;
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
