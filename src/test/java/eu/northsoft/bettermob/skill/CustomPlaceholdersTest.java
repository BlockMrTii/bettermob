package eu.northsoft.bettermob.skill;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CustomPlaceholdersTest {
    private final CustomPlaceholders placeholders = new CustomPlaceholders();
    private final SkillContext context = new SkillContext(null, null, null);

    @Test
    void registeredNamespacesAreResolvedAndOthersLeftAlone() {
        placeholders.register(null, "pet", (key, ctx) -> key.equals("health") ? "20.5" : null);
        assertEquals("heal{a=20.5}", placeholders.apply("heal{a=<pet.health>}", context));
        assertEquals("<pet.unknown> <other.x> <caster.name>", placeholders.apply("<pet.unknown> <other.x> <caster.name>", context));
    }

    @Test
    void valuesCannotInjectSkillSyntaxOrPercentExpansion() {
        placeholders.register(null, "pet", (key, ctx) -> "Rex} - command{c=op x;a=%player%\"");
        String result = placeholders.apply("<pet.name>", context);
        assertFalse(result.matches(".*[{}\\[\\];=\"%].*"), result);
        assertTrue(result.contains("Rex"));
    }

    @Test
    void negativeNumbersKeepTheirSign() {
        placeholders.register(null, "pet", (key, ctx) -> "-5");
        assertEquals("-5", placeholders.apply("<pet.offset>", context));
    }

    @Test
    void casterAndTargetNamespacesAreReservedAndPluginsCanBeRemoved() {
        assertFalse(placeholders.register(null, "caster", (key, ctx) -> "x"));
        assertFalse(placeholders.register(null, "TARGET", (key, ctx) -> "x"));
        assertTrue(placeholders.register(null, "pet", (key, ctx) -> "x"));
        assertFalse(placeholders.register(null, "pet", (key, ctx) -> "y"));
        placeholders.unregister((org.bukkit.plugin.Plugin) null);
        assertTrue(placeholders.isEmpty());
    }

    @Test
    void aFailingPlaceholderLeavesTheTextAsItIsAndIsReported() {
        java.util.List<String> failures = new java.util.ArrayList<>();
        CustomPlaceholders reporting = new CustomPlaceholders((namespace, owner, exception) -> failures.add(namespace + ":" + exception.getMessage()));
        reporting.register(null, "pet", (key, ctx) -> {
            throw new IllegalStateException("boom");
        });
        assertEquals("<Pet.x>", reporting.apply("<Pet.x>", context));
        assertEquals(java.util.List.of("pet:boom"), failures);
    }

    @Test
    void namespacesMatchWithoutCaseSensitivity() {
        placeholders.register(null, "Pet", (key, ctx) -> "20");
        assertEquals("20 20", placeholders.apply("<pet.health> <PET.health>", context));
    }
}
