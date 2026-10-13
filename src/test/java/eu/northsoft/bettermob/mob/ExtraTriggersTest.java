package eu.northsoft.bettermob.mob;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ExtraTriggersTest {
    @AfterEach
    void clear() {
        ExtraTriggers.unregister("swing");
        ExtraTriggers.unregister("pressf");
    }

    @Test
    void registeredNamesAreAcceptedWithOrWithoutTheOnPrefix() {
        assertTrue(ExtraTriggers.register(null, "onSwing"));
        assertTrue(ExtraTriggers.register(null, "PressF"));
        assertFalse(ExtraTriggers.register(null, "swing"));
        assertTrue(ExtraTriggers.accepts("sound{s=x} @self ~onSwing"));
        assertTrue(ExtraTriggers.accepts("skill{s=x} @self ~onPressF ?sneaking"));
        assertTrue(ExtraTriggers.accepts("skill{s=x} @self ~onSwing:5"));
        assertFalse(ExtraTriggers.accepts("skill{s=x} @self ~onDrop"));
    }

    @Test
    void builtInNamesAreRefusedAndUnregisterRemoves() {
        assertFalse(ExtraTriggers.register(null, "onDamaged"));
        assertFalse(ExtraTriggers.register(null, ""));
        ExtraTriggers.register(null, "swing");
        ExtraTriggers.unregister("onSwing");
        assertFalse(ExtraTriggers.accepts("sound{s=x} @self ~onSwing"));
    }

    @Test
    void unparsedLinesOfRegisteredTriggersAreSkippedOnceReady() {
        ExtraTriggers.ready();
        ExtraTriggers.register(null, "swing");
        AtomicInteger warnings = new AtomicInteger();
        ExtraTriggers.warnUnparsed("sound{s=x} @self ~onSwing", warnings::incrementAndGet);
        assertEquals(0, warnings.get());
        ExtraTriggers.warnUnparsed("sound{s=x} @self ~onNothing", warnings::incrementAndGet);
        assertEquals(1, warnings.get());
    }
}
