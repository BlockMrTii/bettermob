package eu.northsoft.bettermob.util;

import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;

class TasksTest {
    @Test
    void runsInlineWhenNotOnFolia() {
        AtomicInteger runs = new AtomicInteger();
        Tasks.runOwned(null, null, runs::incrementAndGet);
        Tasks.runOwnedAt(null, null, runs::incrementAndGet);
        assertEquals(Tasks.FOLIA ? 0 : 2, runs.get());
    }
}
