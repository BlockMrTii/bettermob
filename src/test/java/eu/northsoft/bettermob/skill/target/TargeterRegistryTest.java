package eu.northsoft.bettermob.skill.target;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TargeterRegistryTest {
    private final TargeterRegistry registry = new TargeterRegistry(null);

    @Test
    void customTargetersAreRegisteredOnceAndNeverTakeABuiltinName() {
        assertTrue(registry.registerCustom(null, "petowner", context -> List.of()));
        assertFalse(registry.registerCustom(null, "PetOwner", context -> List.of()));
        assertFalse(registry.registerCustom(null, "self", context -> List.of()));
        assertTrue(registry.has("petowner"));
        registry.unregisterCustom("petowner");
        assertFalse(registry.has("petowner"));
    }
}
