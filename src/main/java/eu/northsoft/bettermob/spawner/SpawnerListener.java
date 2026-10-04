package eu.northsoft.bettermob.spawner;

import com.destroystokyo.paper.event.entity.EntityAddToWorldEvent;
import com.destroystokyo.paper.event.entity.EntityRemoveFromWorldEvent;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;

public final class SpawnerListener implements Listener {
    private final SpawnerManager spawners;

    public SpawnerListener(SpawnerManager spawners) {
        this.spawners = spawners;
    }

    @EventHandler
    public void onAdd(EntityAddToWorldEvent event) {
        spawners.track(event.getEntity());
    }

    @EventHandler
    public void onRemove(EntityRemoveFromWorldEvent event) {
        spawners.untrack(event.getEntity());
    }
}
