package eu.northsoft.bettermob;

import org.bukkit.Bukkit;
import org.bukkit.entity.Entity;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitTask;

/**
 * Einheitlicher Zugang zum Scheduler: auf Folia laufen Aufgaben im Region-Thread des
 * jeweiligen Entities bzw. im Global-Region-Scheduler (der Bukkit-Scheduler wirft dort
 * UnsupportedOperationException), auf normalem Paper bleibt alles beim Bukkit-Scheduler.
 */
final class Tasks {
    static final boolean FOLIA = detectFolia();

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

    /** Einmalig nach "ticks" auf dem Thread des Entities. Entfaellt, wenn das Entity bis dahin weg ist. */
    static void runLater(Plugin plugin, Entity entity, long ticks, Runnable task) {
        if (FOLIA) {
            entity.getScheduler().runDelayed(plugin, scheduled -> task.run(), null, Math.max(1, ticks));
            return;
        }
        // isValid() ist beim Tod schon false, obwohl der Entity-Body fuer die Sterbeanimation
        // noch ein paar Ticks da ist - deshalb ueber die Registry pruefen.
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            if (Bukkit.getEntity(entity.getUniqueId()) != null) task.run();
        }, ticks);
    }

    /** Wiederholt auf dem Thread des Entities; liefert die Aktion zum Abbrechen. */
    static Runnable runTimer(Plugin plugin, Entity entity, long delay, long period, Runnable task) {
        if (FOLIA) {
            var scheduled = entity.getScheduler().runAtFixedRate(plugin, t -> task.run(), null, Math.max(1, delay), Math.max(1, period));
            return scheduled == null ? () -> { } : scheduled::cancel;
        }
        BukkitTask bukkitTask = Bukkit.getScheduler().runTaskTimer(plugin, task, delay, period);
        return bukkitTask::cancel;
    }

    /** Einmal im naechsten Tick auf dem Global-/Hauptthread, z.B. fuer Konsolenbefehle. */
    static void runGlobal(Plugin plugin, Runnable task) {
        if (FOLIA) Bukkit.getGlobalRegionScheduler().run(plugin, scheduled -> task.run());
        else Bukkit.getScheduler().runTask(plugin, task);
    }
}
