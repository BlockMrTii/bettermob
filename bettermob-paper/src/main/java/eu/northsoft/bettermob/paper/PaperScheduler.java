package eu.northsoft.bettermob.paper;

import eu.northsoft.bettermob.platform.Cancellable;
import eu.northsoft.bettermob.platform.PlatformEntity;
import eu.northsoft.bettermob.platform.PlatformScheduler;
import eu.northsoft.bettermob.platform.Position;
import eu.northsoft.bettermob.util.Tasks;
import org.bukkit.Location;
import org.bukkit.plugin.Plugin;

/** {@link PlatformScheduler} backed by {@link Tasks} (Bukkit scheduler or Folia region schedulers). */
public final class PaperScheduler implements PlatformScheduler {
    private final Plugin plugin;

    public PaperScheduler(Plugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public void runLater(PlatformEntity entity, long ticks, Runnable task, Runnable retired) {
        Tasks.runLater(plugin, PaperAdapters.bukkit(entity), ticks, task, retired);
    }

    @Override
    public Cancellable runTimer(PlatformEntity entity, long delay, long period, Runnable task) {
        return Tasks.runTimer(plugin, PaperAdapters.bukkit(entity), delay, period, task)::run;
    }

    @Override
    public Cancellable runGlobalTimer(long period, Runnable task) {
        return Tasks.runGlobalTimer(plugin, period, task)::run;
    }

    @Override
    public boolean runOwned(PlatformEntity entity, Runnable task, Runnable retired) {
        return Tasks.runOwned(plugin, PaperAdapters.bukkit(entity), task, retired);
    }

    @Override
    public boolean runOwnedAt(Position position, Runnable task, Runnable rejected) {
        Location location = PaperAdapters.location(position);
        if (location == null) {
            if (rejected != null) rejected.run();
            return false;
        }
        return Tasks.runOwnedAt(plugin, location, task, rejected);
    }

    @Override
    public void runGlobal(Runnable task) {
        Tasks.runGlobal(plugin, task);
    }

    @Override
    public void runAsync(Runnable task) {
        Tasks.runAsync(plugin, task);
    }
}
