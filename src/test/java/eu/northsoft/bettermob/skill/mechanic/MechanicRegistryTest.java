package eu.northsoft.bettermob.skill.mechanic;

import eu.northsoft.bettermob.api.CustomMechanic;
import org.bukkit.plugin.Plugin;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MechanicRegistryTest {
    private static final List<String> FORMER_BUILTIN_NAMES = List.of(
            "cancelskill", "cancelevent", "message", "msg", "skill", "look", "sound", "state", "potion", "breakblock",
            "gcd", "model", "modelengine", "randomskill", "remove", "command", "summon", "mountmodel", "delay",
            "effect:particles", "e:p", "particles", "effect:particlering", "spin", "takeitem", "sudoskill", "damage",
            "throw", "lunge", "setblock", "equip", "aura", "ondamaged", "onattack", "ontick", "ondeath", "onshoot",
            "bodyrotation", "addtag", "removetag", "ignite", "totem", "velocity", "freeze", "shoot", "stun",
            "setnodamageticks");

    private static final Set<String> HANDLED_BY_ENGINE = Set.of("cancelskill", "delay");

    private static final CustomMechanic NOTHING = context -> { };

    private MechanicRegistry registry;

    @BeforeEach
    void setUp() {
        registry = new MechanicRegistry(null);
        BuiltinMechanics.registerAll(registry, null);
    }

    @Test
    void everyFormerBuiltinNameStaysTaken() {
        for (String name : FORMER_BUILTIN_NAMES) {
            assertFalse(registry.registerCustom((Plugin) null, name, NOTHING), name);
        }
    }

    @Test
    void everyFormerBuiltinNameExceptEngineHandledOnesHasAMechanic() {
        for (String name : FORMER_BUILTIN_NAMES) {
            if (HANDLED_BY_ENGINE.contains(name)) assertNull(registry.get(name), name);
            else assertNotNull(registry.get(name), name);
        }
    }

    @Test
    void namesAreMatchedCaseInsensitivelyWhenRegistering() {
        assertFalse(registry.registerCustom(null, "SOUND", NOTHING));
        assertFalse(registry.registerCustom(null, "Delay", NOTHING));
    }

    @Test
    void customMechanicsCanBeAddedOnceAndRemoved() {
        assertTrue(registry.registerCustom(null, "mine", NOTHING));
        assertFalse(registry.registerCustom(null, "mine", NOTHING));
        assertNotNull(registry.get("mine"));

        registry.unregisterCustom("mine");
        assertNull(registry.get("mine"));
        assertTrue(registry.registerCustom(null, "mine", NOTHING));
    }

    @Test
    void builtinMechanicsCannotBeUnregistered() {
        registry.unregisterCustom("sound");
        assertNotNull(registry.get("sound"));
    }

    @Test
    void aliasesShareOneMechanic() {
        assertEquals(registry.get("message"), registry.get("msg"));
        assertEquals(registry.get("aura"), registry.get("onshoot"));
        assertEquals(registry.get("effect:particles"), registry.get("e:p"));
    }
}
