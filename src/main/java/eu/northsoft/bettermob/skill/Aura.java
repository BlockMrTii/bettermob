package eu.northsoft.bettermob.skill;

final class Aura {
final String kind;
final long until;
final boolean cancelEvent;
final String onEnd;
final String onHit;
Runnable cancelTicker = () -> { };
Runnable cancelEnd = () -> { };

Aura(String kind, long until, boolean cancelEvent, String onEnd, String onHit) {
        this.kind = kind;
        this.until = until;
        this.cancelEvent = cancelEvent;
        this.onEnd = onEnd;
        this.onHit = onHit;
    }

void stop() {
        cancelTicker.run();
        cancelEnd.run();
    }
}
