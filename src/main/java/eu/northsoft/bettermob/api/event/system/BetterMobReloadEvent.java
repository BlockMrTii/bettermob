package eu.northsoft.bettermob.api.event.system;

import java.util.List;
import java.util.Map;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;

public class BetterMobReloadEvent extends Event {
    private static final HandlerList HANDLERS = new HandlerList();

    private final Map<String, Changes> changes;
    private final int updatedMobs;

    public BetterMobReloadEvent(Map<String, Changes> changes, int updatedMobs) {
        this.changes = changes;
        this.updatedMobs = updatedMobs;
    }

    public Map<String, Changes> getChanges() {
        return changes;
    }

    public int getUpdatedMobs() {
        return updatedMobs;
    }

    public record Changes(List<String> added, List<String> removed, List<String> changed) {
        public boolean isEmpty() {
            return added.isEmpty() && removed.isEmpty() && changed.isEmpty();
        }
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
