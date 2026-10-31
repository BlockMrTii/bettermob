package eu.northsoft.bettermob.fabric;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class FabricSpawnerManagerTest {

    @Test
    void loadsSpawnersFromYamlFile(@TempDir Path tempDir) throws IOException {
        Path spawnersYml = tempDir.resolve("spawners.yml");
        String yaml = """
                graveyard_knight:
                  mob: skeleton_knight
                  world: minecraft:overworld
                  x: 100.5
                  y: 65.0
                  z: -200.5
                  radius: 8
                  interval: 15
                  max: 4
                  player-range: 40
                """;
        Files.writeString(spawnersYml, yaml);

        BetterMobFabricMod mod = new BetterMobFabricMod();
        FabricSpawnerManager manager = new FabricSpawnerManager(mod, spawnersYml);
        manager.load();

        assertEquals(1, manager.spawners().size());
        FabricSpawnerManager.ActiveSpawner spawner = manager.spawners().get("graveyard_knight");
        assertNotNull(spawner);
        assertEquals("skeleton_knight", spawner.definition().mob());
        assertEquals("minecraft:overworld", spawner.definition().world());
        assertEquals(100.5, spawner.definition().x());
        assertEquals(65.0, spawner.definition().y());
        assertEquals(-200.5, spawner.definition().z());
        assertEquals(8, spawner.definition().radius());
        assertEquals(15, spawner.definition().intervalSeconds());
        assertEquals(4, spawner.definition().max());
        assertEquals(40, spawner.definition().playerRange());
    }
}
