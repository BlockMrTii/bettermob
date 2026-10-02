package eu.northsoft.bettermob.util;

import org.bukkit.Bukkit;
import org.bukkit.entity.Entity;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitTask;

public final class Tasks {
    public static final boolean FOLIA = detectFolia();

    private Tasks() {
    }

    private static boolean detectFolia() {
        try {
            Class.forName("io.papermc.paper.threadedregions.RegionizedServer");
            return true;
        } catch (ClassNotFoundException exception) {
            return false;
        }
    }

    public static void runLater(Plugin plugin, Entity entity, long ticks, Runnable task) {
        if (FOLIA) {
            entity.getScheduler().runDelayed(plugin, scheduled -> task.run(), null, Math.max(1, ticks));
            return;
        }

        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            if (Bukkit.getEntity(entity.getUniqueId()) != null) task.run();
        }, ticks);
    }

    public static Runnable runTimer(Plugin plugin, Entity entity, long delay, long period, Runnable task) {
        if (FOLIA) {
            var scheduled = entity.getScheduler().runAtFixedRate(plugin, t -> task.run(), null, Math.max(1, delay), Math.max(1, period));
            return scheduled == null ? () -> { } : scheduled::cancel;
        }
        BukkitTask bukkitTask = Bukkit.getScheduler().runTaskTimer(plugin, task, delay, period);
        return bukkitTask::cancel;
    }

    public static void runGlobal(Plugin plugin, Runnable task) {
        if (FOLIA) Bukkit.getGlobalRegionScheduler().run(plugin, scheduled -> task.run());
        else Bukkit.getScheduler().runTask(plugin, task);
    }
}
