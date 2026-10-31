package eu.northsoft.bettermob.fabric;

import eu.northsoft.bettermob.platform.Cancellable;
import eu.northsoft.bettermob.platform.PlatformEntity;
import eu.northsoft.bettermob.platform.PlatformScheduler;
import eu.northsoft.bettermob.platform.Position;

public final class FabricScheduler implements PlatformScheduler {
    @Override
    public void runLater(PlatformEntity entity, long ticks, Runnable task, Runnable retired) {
        // Will be wired to ServerTickEvents queue
    }

    @Override
    public Cancellable runTimer(PlatformEntity entity, long delay, long period, Runnable task) {
        return Cancellable.NONE;
    }

    @Override
    public Cancellable runGlobalTimer(long period, Runnable task) {
        return Cancellable.NONE;
    }

    @Override
    public boolean runOwned(PlatformEntity entity, Runnable task, Runnable retired) {
        task.run();
        return true;
    }

    @Override
    public boolean runOwnedAt(Position position, Runnable task, Runnable rejected) {
        task.run();
        return true;
    }

    @Override
    public void runGlobal(Runnable task) {
        task.run();
    }

    @Override
    public void runAsync(Runnable task) {
        Thread.startVirtualThread(task);
    }
}
