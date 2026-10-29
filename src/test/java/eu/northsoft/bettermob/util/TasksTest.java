package eu.northsoft.bettermob.util;

import org.bukkit.Bukkit;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitScheduler;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class TasksTest {
    @Test
    void runsInlineWhenNotOnFolia() {
        AtomicInteger runs = new AtomicInteger();
        Tasks.runOwned(null, null, runs::incrementAndGet);
        Tasks.runOwnedAt(null, null, runs::incrementAndGet);
        assertEquals(Tasks.FOLIA ? 0 : 2, runs.get());
    }

    @Test
    void schedulesToPrimaryThreadWhenAsyncOnPaper() {
        if (Tasks.FOLIA) return;
        BukkitScheduler scheduler = mock(BukkitScheduler.class);
        Plugin plugin = mock(Plugin.class);
        when(plugin.isEnabled()).thenReturn(true);
        AtomicInteger runs = new AtomicInteger();
        Runnable task = runs::incrementAndGet;

        try (MockedStatic<Bukkit> bukkit = mockStatic(Bukkit.class)) {
            bukkit.when(Bukkit::isPrimaryThread).thenReturn(false);
            bukkit.when(Bukkit::getScheduler).thenReturn(scheduler);

            Tasks.runOn(plugin, null, task);
            verify(scheduler).runTask(plugin, task);
            assertEquals(0, runs.get());

            Tasks.runAt(plugin, null, task);
            verify(scheduler, times(2)).runTask(plugin, task);
            assertEquals(0, runs.get());
        }
    }

    @Test
    void doesNotScheduleWhenPluginDisabledDuringShutdown() {
        if (Tasks.FOLIA) return;
        BukkitScheduler scheduler = mock(BukkitScheduler.class);
        Plugin plugin = mock(Plugin.class);
        when(plugin.isEnabled()).thenReturn(false);
        AtomicInteger runs = new AtomicInteger();
        Runnable task = runs::incrementAndGet;

        try (MockedStatic<Bukkit> bukkit = mockStatic(Bukkit.class)) {
            bukkit.when(Bukkit::isPrimaryThread).thenReturn(false);
            bukkit.when(Bukkit::getScheduler).thenReturn(scheduler);

            Tasks.runOn(plugin, null, task);
            verify(scheduler, never()).runTask(any(Plugin.class), any(Runnable.class));
            assertEquals(0, runs.get());
        }
    }

    @Test
    void runsInlineWhenPrimaryThreadOnPaper() {
        if (Tasks.FOLIA) return;
        Plugin plugin = mock(Plugin.class);
        AtomicInteger runs = new AtomicInteger();
        Runnable task = runs::incrementAndGet;

        try (MockedStatic<Bukkit> bukkit = mockStatic(Bukkit.class)) {
            bukkit.when(Bukkit::isPrimaryThread).thenReturn(true);

            Tasks.runOn(plugin, null, task);
            Tasks.runAt(plugin, null, task);
            assertEquals(2, runs.get());
        }
    }
}
