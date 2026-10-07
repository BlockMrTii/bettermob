package eu.northsoft.bettermob.skill;

public final class Aura {
    public final String kind;
    public final long until;
    public final boolean cancelEvent;
    public final String onEnd;
    public final String onHit;
    public volatile Runnable cancelTicker = () -> { };
    public volatile Runnable cancelEnd = () -> { };
    private volatile boolean stopped;

    public Aura(String kind, long until, boolean cancelEvent, String onEnd, String onHit) {
        this.kind = kind;
        this.until = until;
        this.cancelEvent = cancelEvent;
        this.onEnd = onEnd;
        this.onHit = onHit;
    }

    public void bindTicker(Runnable cancel) {
        cancelTicker = cancel;
        if (stopped) cancel.run();
    }

    public void stop() {
        stopped = true;
        cancelTicker.run();
        cancelEnd.run();
    }
}
