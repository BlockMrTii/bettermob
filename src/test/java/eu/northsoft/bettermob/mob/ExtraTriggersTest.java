package eu.northsoft.bettermob.mob;

import org.bukkit.plugin.Plugin;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Proxy;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ExtraTriggersTest {
    private static final Plugin OWNER = (Plugin) Proxy.newProxyInstance(Plugin.class.getClassLoader(), new Class<?>[]{Plugin.class}, (proxy, method, args) -> {
        if (method.getName().equals("hashCode")) return 1;
        if (method.getName().equals("equals")) return proxy == args[0];
        return null;
    });

    @AfterEach
    void clear() {
        ExtraTriggers.reset();
    }

    @Test
    void registeredNamesAreAcceptedWithOrWithoutTheOnPrefix() {
        assertTrue(ExtraTriggers.register(OWNER, "onWave"));
        assertTrue(ExtraTriggers.register(OWNER, "Whistle"));
        assertFalse(ExtraTriggers.register(OWNER, "wave"));
        assertTrue(ExtraTriggers.accepts("sound{s=x} @self ~onWave"));
        assertTrue(ExtraTriggers.accepts("skill{s=x} @self ~onWhistle ?sneaking"));
        assertTrue(ExtraTriggers.accepts("skill{s=x} @self ~onWave:5"));
        assertFalse(ExtraTriggers.accepts("skill{s=x} @self ~onDrop"));
    }

    @Test
    void aLineWithoutASkillStepIsNotAccepted() {
        ExtraTriggers.register(OWNER, "wave");
        assertFalse(ExtraTriggers.accepts("~onWave"));
        assertFalse(ExtraTriggers.accepts("   ~onWave ?sneaking"));
    }

    @Test
    void builtInNamesAndMissingOwnersAreRefusedAndUnregisterRemoves() {
        assertFalse(ExtraTriggers.register(OWNER, "onDamaged"));
        assertFalse(ExtraTriggers.register(OWNER, ""));
        assertFalse(ExtraTriggers.register(null, "wave"));
        ExtraTriggers.register(OWNER, "wave");
        ExtraTriggers.unregister("onWave");
        assertFalse(ExtraTriggers.accepts("sound{s=x} @self ~onWave"));
        ExtraTriggers.register(OWNER, "wave");
        ExtraTriggers.unregister(OWNER);
        assertFalse(ExtraTriggers.accepts("sound{s=x} @self ~onWave"));
    }

    @Test
    void unparsedLinesAreHeldUntilReadyAndResetStartsOver() {
        AtomicInteger warnings = new AtomicInteger();
        ExtraTriggers.register(OWNER, "wave");
        ExtraTriggers.warnUnparsed("sound{s=x} @self ~onNothing", warnings::incrementAndGet);
        assertEquals(0, warnings.get());
        ExtraTriggers.ready();
        assertEquals(1, warnings.get());
        ExtraTriggers.warnUnparsed("sound{s=x} @self ~onWave", warnings::incrementAndGet);
        assertEquals(1, warnings.get());
        ExtraTriggers.warnUnparsed("sound{s=x} @self ~onOther", warnings::incrementAndGet);
        assertEquals(2, warnings.get());
        ExtraTriggers.reset();
        ExtraTriggers.warnUnparsed("sound{s=x} @self ~onOther", warnings::incrementAndGet);
        assertEquals(2, warnings.get());
    }
}
