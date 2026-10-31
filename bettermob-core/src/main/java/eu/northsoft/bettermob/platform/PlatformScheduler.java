package eu.northsoft.bettermob.platform;

/**
 * Tick scheduler. On region-threaded servers (Folia) entity/position tasks run on the owning
 * region; elsewhere everything runs on the main server thread.
 */
public interface PlatformScheduler {
    /** Runs on the entity's thread after {@code ticks}; {@code retired} runs instead if the entity is gone. */
    void runLater(PlatformEntity entity, long ticks, Runnable task, Runnable retired);

    default void runLater(PlatformEntity entity, long ticks, Runnable task) {
        runLater(entity, ticks, task, null);
    }

    Cancellable runTimer(PlatformEntity entity, long delay, long period, Runnable task);

    Cancellable runGlobalTimer(long period, Runnable task);

    /** Runs now if already on the owning thread, otherwise schedules it. Returns {@code true} if it ran now. */
    boolean runOwned(PlatformEntity entity, Runnable task, Runnable retired);

    default boolean runOwned(PlatformEntity entity, Runnable task) {
        return runOwned(entity, task, null);
    }

    boolean runOwnedAt(Position position, Runnable task, Runnable rejected);

    default boolean runOwnedAt(Position position, Runnable task) {
        return runOwnedAt(position, task, null);
    }

    void runGlobal(Runnable task);

    void runAsync(Runnable task);
}
