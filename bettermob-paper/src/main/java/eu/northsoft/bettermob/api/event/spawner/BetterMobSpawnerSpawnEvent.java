package eu.northsoft.bettermob.api.event.spawner;

import org.bukkit.Location;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;

public class BetterMobSpawnerSpawnEvent extends Event implements Cancellable {
    private static final HandlerList HANDLERS = new HandlerList();

    private final String spawnerId;
    private final String mobId;
    private final Location location;
    private boolean cancelled;

    public BetterMobSpawnerSpawnEvent(String spawnerId, String mobId, Location location) {
        this.spawnerId = spawnerId;
        this.mobId = mobId;
        this.location = location;
    }

    public String getSpawnerId() {
        return spawnerId;
    }

    public String getMobId() {
        return mobId;
    }

    public Location getLocation() {
        return location;
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
