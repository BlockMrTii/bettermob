package eu.northsoft.bettermob.platform;

/** Handle to a scheduled task. */
@FunctionalInterface
public interface Cancellable {
    Cancellable NONE = () -> { };

    void cancel();
}
