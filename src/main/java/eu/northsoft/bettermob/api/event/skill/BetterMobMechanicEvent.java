package eu.northsoft.bettermob.api.event.skill;

import eu.northsoft.bettermob.api.MobInfo;
import java.util.Map;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;

public class BetterMobMechanicEvent extends Event implements Cancellable {
    private static final HandlerList HANDLERS = new HandlerList();

    private final LivingEntity caster;
    private final String mechanic;
    private final Map<String, String> params;
    private final Entity target;
    private final MobInfo mob;
    private boolean cancelled;

    public BetterMobMechanicEvent(LivingEntity caster, String mechanic, Map<String, String> params, Entity target, MobInfo mob) {
        this.caster = caster;
        this.mechanic = mechanic;
        this.params = params;
        this.target = target;
        this.mob = mob;
    }

    public LivingEntity getCaster() {
        return caster;
    }

    public String getMechanic() {
        return mechanic;
    }

    public Map<String, String> getParams() {
        return params;
    }

    public Entity getTarget() {
        return target;
    }

    public MobInfo getMob() {
        return mob;
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
