package eu.northsoft.bettermob.skill;

public final class Aura {
    public final String kind;
    public final long until;
    public final boolean cancelEvent;
    public final String onEnd;
    public final String onHit;
    public Runnable cancelTicker = () -> { };
    public Runnable cancelEnd = () -> { };

    public Aura(String kind, long until, boolean cancelEvent, String onEnd, String onHit) {
        this.kind = kind;
        this.until = until;
        this.cancelEvent = cancelEvent;
        this.onEnd = onEnd;
        this.onHit = onHit;
    }

    public void stop() {
        cancelTicker.run();
        cancelEnd.run();
    }
}
