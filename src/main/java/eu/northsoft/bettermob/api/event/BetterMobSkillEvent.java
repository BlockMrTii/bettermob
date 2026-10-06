package eu.northsoft.bettermob.api.event;

import eu.northsoft.bettermob.api.MobInfo;
import org.bukkit.entity.LivingEntity;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;

public class BetterMobSkillEvent extends Event implements Cancellable {
    private static final HandlerList HANDLERS = new HandlerList();

    private final String skillId;
    private final LivingEntity caster;
    private final LivingEntity trigger;
    private final MobInfo mob;
    private boolean cancelled;

    public BetterMobSkillEvent(String skillId, LivingEntity caster, LivingEntity trigger, MobInfo mob) {
        this.skillId = skillId;
        this.caster = caster;
        this.trigger = trigger;
        this.mob = mob;
    }

    public String getSkillId() {
        return skillId;
    }

    public LivingEntity getCaster() {
        return caster;
    }

    public LivingEntity getTrigger() {
        return trigger;
    }

    public MobInfo getMob() {
        return mob;
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
