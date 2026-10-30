package eu.northsoft.bettermob.api.event.item;

import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

public class BetterMobItemUseEvent extends Event implements Cancellable {
    private static final HandlerList HANDLERS = new HandlerList();

    private final Player player;
    private final String itemId;
    private final ItemStack item;
    private boolean cancelled;

    public BetterMobItemUseEvent(Player player, String itemId, ItemStack item) {
        this.player = player;
        this.itemId = itemId;
        this.item = item;
    }

    public Player getPlayer() {
        return player;
    }

    public String getItemId() {
        return itemId;
    }

    public ItemStack getItem() {
        return item;
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
