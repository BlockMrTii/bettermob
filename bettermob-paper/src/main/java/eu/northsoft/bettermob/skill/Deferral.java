package eu.northsoft.bettermob.skill;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

public final class Deferral {
    private final AtomicInteger outstanding = new AtomicInteger();
    private final AtomicBoolean resumed = new AtomicBoolean();
    private volatile Runnable continuation;

    public void add() {
        outstanding.incrementAndGet();
    }

    public void complete() {
        if (outstanding.decrementAndGet() == 0) resumeIfReady();
    }

    public boolean await(Runnable next) {
        if (outstanding.get() == 0) return false;
        continuation = next;
        resumeIfReady();
        return true;
    }

    private void resumeIfReady() {
        Runnable next = continuation;
        if (next != null && outstanding.get() == 0 && resumed.compareAndSet(false, true)) next.run();
    }
}
