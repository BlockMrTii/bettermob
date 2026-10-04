package eu.northsoft.bettermob.skill;

import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DeferralTest {
    @Test
    void continuesInlineWhenNothingIsOutstanding() {
        AtomicInteger resumed = new AtomicInteger();
        assertFalse(new Deferral().await(resumed::incrementAndGet));
        assertEquals(0, resumed.get());
    }

    @Test
    void completedInlineWorkDoesNotDefer() {
        Deferral deferral = new Deferral();
        deferral.add();
        deferral.complete();
        assertFalse(deferral.await(() -> { }));
    }

    @Test
    void resumesOnceAfterTheLastOutstandingWork() {
        Deferral deferral = new Deferral();
        AtomicInteger resumed = new AtomicInteger();
        deferral.add();
        deferral.add();
        assertTrue(deferral.await(resumed::incrementAndGet));
        deferral.complete();
        assertEquals(0, resumed.get());
        deferral.complete();
        assertEquals(1, resumed.get());
    }

    @Test
    void resumesWhenTheWorkFinishedBeforeTheAwait() {
        Deferral deferral = new Deferral();
        AtomicInteger resumed = new AtomicInteger();
        deferral.add();
        deferral.add();
        deferral.complete();
        assertTrue(deferral.await(resumed::incrementAndGet));
        deferral.complete();
        assertEquals(1, resumed.get());
    }
}
