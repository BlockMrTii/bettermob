package eu.northsoft.bettermob.stats;

import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class KillStatsTest {
    private final UUID alice = UUID.fromString("00000000-0000-0000-0000-00000000000a");
    private final UUID bob = UUID.fromString("00000000-0000-0000-0000-00000000000b");

    @Test
    void killsAreCountedPerPlayerAndMobCaseInsensitively() {
        KillStats stats = new KillStats();
        stats.record(alice, "Alice", "Goblin");
        stats.record(alice, "Alice", "goblin");
        stats.record(alice, "Alice", "orc");
        assertEquals(3, stats.total(alice));
        assertEquals(2, stats.count(alice, "GOBLIN"));
        assertEquals(0, stats.count(alice, "troll"));
        assertEquals(0, stats.total(bob));
    }

    @Test
    void theTopListIsSortedByKillsThenName() {
        KillStats stats = new KillStats();
        for (int i = 0; i < 3; i++) stats.record(bob, "bob", "goblin");
        for (int i = 0; i < 3; i++) stats.record(alice, "Alice", "goblin");
        stats.record(UUID.randomUUID(), "Carl", "orc");
        List<KillStats.Row> top = stats.top(null, 10);
        assertEquals(List.of("Alice", "bob", "Carl"), top.stream().map(KillStats.Row::name).toList());
        assertEquals(List.of("Alice", "bob"), stats.top("goblin", 10).stream().map(KillStats.Row::name).toList());
        assertEquals(1, stats.top(null, 1).size());
        assertTrue(stats.top("troll", 10).isEmpty());
    }

    @Test
    void theTopListIsEmptyForAZeroLimit() {
        KillStats stats = new KillStats();
        stats.record(alice, "Alice", "goblin");
        assertTrue(stats.top(null, 0).isEmpty());
    }

    @Test
    void dataSurvivesASaveAndLoad(@TempDir File folder) throws IOException {
        KillStats stats = new KillStats();
        stats.record(alice, "Alice", "goblin");
        stats.record(alice, "Alice", "goblin");
        stats.record(bob, "Bob", "orc");
        assertTrue(stats.dirty());
        File file = new File(folder, "kills.yml");
        stats.flush(file);
        assertFalse(stats.dirty());

        KillStats loaded = new KillStats();
        loaded.load(YamlConfiguration.loadConfiguration(file));
        assertEquals(2, loaded.count(alice, "goblin"));
        assertEquals(1, loaded.total(bob));
        assertEquals("Alice", loaded.top(null, 1).get(0).name());
        assertFalse(loaded.dirty());
    }

    @Test
    void loadingIgnoresBrokenEntries() {
        YamlConfiguration config = new YamlConfiguration();
        config.set("players.not-a-uuid.kills.goblin", 5);
        config.set("players." + alice + ".kills.goblin", 0);
        config.set("players." + bob + ".kills.orc", 2);
        KillStats stats = new KillStats();
        stats.load(config);
        assertEquals(0, stats.total(alice));
        assertEquals(2, stats.total(bob));
        assertEquals(1, stats.top(null, 10).size());
    }

    @Test
    void aProtectedStoreNeverOverwritesTheFile(@TempDir File folder) throws IOException {
        KillStats stats = new KillStats();
        File file = new File(folder, "kills.yml");
        java.nio.file.Files.writeString(file.toPath(), "keep: me\n");
        stats.protect();
        stats.record(alice, "Alice", "goblin");
        stats.flush(file);
        assertEquals("keep: me\n", java.nio.file.Files.readString(file.toPath()));
    }
}
