package eu.northsoft.bettermob.fabric;

import eu.northsoft.bettermob.mob.MobDefinition;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FabricMobRegistryTest {

    @Test
    void loadsMobsFromConfigDirectory(@TempDir Path tempDir) throws IOException {
        Path mobsDir = tempDir.resolve("mobs");
        Files.createDirectories(mobsDir);

        String mobYaml = """
                Type: SKELETON
                Display: '&cTest Knight'
                Model: test_knight
                Health: 80
                Damage: 10
                Options:
                  Collidable: true
                  MovementSpeed: 0.3
                DamageModifiers:
                  - FIRE 0.5
                """;

        Files.writeString(mobsDir.resolve("test_knight.yml"), mobYaml);

        FabricMobRegistry registry = new FabricMobRegistry(mobsDir);
        registry.load();

        assertEquals(1, registry.all().size());
        MobDefinition mob = registry.get("test_knight");
        assertNotNull(mob);
        assertEquals("test_knight", mob.id);
        assertEquals("SKELETON", mob.type);
        assertEquals("&cTest Knight", mob.displayName);
        assertEquals("test_knight", mob.modelId);
        assertEquals(80.0, mob.health);
        assertEquals(10.0, mob.damage);
        assertEquals(0.3, mob.options.movementSpeed());
        assertEquals(0.5, mob.damageModifiers.get("FIRE"));
    }

    @Test
    void extractsDefaultTemplatesIfDirectoryDoesNotExist(@TempDir Path tempDir) {
        Path mobsDir = tempDir.resolve("mobs");
        FabricMobRegistry registry = new FabricMobRegistry(mobsDir);
        registry.load();

        assertTrue(Files.isDirectory(mobsDir));
        assertTrue(registry.all().size() >= 2);
        assertNotNull(registry.get("skeleton_knight"));
        assertNotNull(registry.get("zombie_brute"));
    }
}
