package eu.northsoft.bettermob.api.event.mob;

import eu.northsoft.bettermob.api.MobInfo;
import java.util.List;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

public class BetterMobDropEvent extends Event {
    private static final HandlerList HANDLERS = new HandlerList();

    private final LivingEntity entity;
    private final MobInfo mob;
    private final Player killer;
    private final List<ItemStack> drops;
    private int exp;

    public BetterMobDropEvent(LivingEntity entity, MobInfo mob, Player killer, List<ItemStack> drops, int exp) {
        this.entity = entity;
        this.mob = mob;
        this.killer = killer;
        this.drops = drops;
        this.exp = exp;
    }

    public LivingEntity getEntity() {
        return entity;
    }

    public MobInfo getMob() {
        return mob;
    }

    public Player getKiller() {
        return killer;
    }

    public List<ItemStack> getDrops() {
        return drops;
    }

    public int getExp() {
        return exp;
    }

    public void setExp(int exp) {
        this.exp = exp;
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
