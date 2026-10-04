package eu.northsoft.bettermob.spawner;

import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SpawnerTest {
    @Test
    void writtenSpawnerReadsBackIdentically() {
        YamlConfiguration yaml = new YamlConfiguration();
        new Spawner("camp", "goblin", "world", 10.5, 64, -3.5, 7, 45, 4, 40).write(yaml.createSection("camp"));
        Spawner read = Spawner.read("camp", yaml.getConfigurationSection("camp"));
        assertEquals("goblin", read.mob);
        assertEquals("world", read.world);
        assertEquals(10.5, read.x);
        assertEquals(-3.5, read.z);
        assertEquals(7, read.radius);
        assertEquals(45, read.intervalSeconds);
        assertEquals(4, read.max);
        assertEquals(40, read.playerRange);
    }

    @Test
    void missingOptionsFallBackToDefaults() {
        YamlConfiguration yaml = new YamlConfiguration();
        yaml.set("camp.mob", "goblin");
        yaml.set("camp.world", "world");
        Spawner read = Spawner.read("camp", yaml.getConfigurationSection("camp"));
        assertEquals(Spawner.DEFAULT_RADIUS, read.radius);
        assertEquals(Spawner.DEFAULT_MAX, read.max);
        assertEquals(Spawner.DEFAULT_PLAYER_RANGE, read.playerRange);
    }

    @Test
    void entryWithoutMobOrWorldIsRejected() {
        YamlConfiguration yaml = new YamlConfiguration();
        yaml.set("camp.mob", "goblin");
        assertNull(Spawner.read("camp", yaml.getConfigurationSection("camp")));
    }

    @Test
    void spawnsOnlyWhenTheIntervalPassedAndBelowTheMax() {
        Spawner spawner = new Spawner("camp", "goblin", "world", 0, 0, 0, 5, 30, 3, 32);
        assertTrue(spawner.ready(0, 2));
        assertFalse(spawner.ready(0, 3));
        spawner.nextSpawnAt = 1000;
        assertFalse(spawner.ready(999, 0));
        assertTrue(spawner.ready(1000, 0));
    }
}
