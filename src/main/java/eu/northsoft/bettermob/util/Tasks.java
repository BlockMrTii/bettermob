package eu.northsoft.bettermob.util;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
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
            if (Bukkit.getEntity(entity.getUniqueId()) != null || isDeadLiving(entity)) task.run();
        }, ticks);
    }

    private static boolean isDeadLiving(Entity entity) {
        return entity instanceof LivingEntity living && living.getHealth() <= 0;
    }

    public static Runnable runTimer(Plugin plugin, Entity entity, long delay, long period, Runnable task) {
        if (FOLIA) {
            var scheduled = entity.getScheduler().runAtFixedRate(plugin, t -> task.run(), null, Math.max(1, delay), Math.max(1, period));
            return scheduled == null ? () -> { } : scheduled::cancel;
        }
        BukkitTask bukkitTask = Bukkit.getScheduler().runTaskTimer(plugin, task, delay, period);
        return bukkitTask::cancel;
    }

    public static Runnable runGlobalTimer(Plugin plugin, long period, Runnable task) {
        if (FOLIA) {
            var scheduled = Bukkit.getGlobalRegionScheduler().runAtFixedRate(plugin, t -> task.run(), period, period);
            return scheduled::cancel;
        }
        BukkitTask bukkitTask = Bukkit.getScheduler().runTaskTimer(plugin, task, period, period);
        return bukkitTask::cancel;
    }

    public static void runAt(Plugin plugin, Location location, Runnable task) {
        if (FOLIA) Bukkit.getRegionScheduler().run(plugin, location, scheduled -> task.run());
        else task.run();
    }

    public static void runOwned(Plugin plugin, Entity entity, Runnable task) {
        if (!FOLIA || Bukkit.isOwnedByCurrentRegion(entity)) task.run();
        else entity.getScheduler().run(plugin, scheduled -> task.run(), null);
    }

    public static void runOwnedAt(Plugin plugin, Location location, Runnable task) {
        if (!FOLIA || Bukkit.isOwnedByCurrentRegion(location)) task.run();
        else Bukkit.getRegionScheduler().run(plugin, location, scheduled -> task.run());
    }

    public static void runGlobal(Plugin plugin, Runnable task) {
        if (FOLIA) Bukkit.getGlobalRegionScheduler().run(plugin, scheduled -> task.run());
        else Bukkit.getScheduler().runTask(plugin, task);
    }
}
