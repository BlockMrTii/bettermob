package eu.northsoft.bettermob.command;

import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.util.Set;
import java.util.TreeSet;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PermissionsTest {
    private static final YamlConfiguration PLUGIN =
            YamlConfiguration.loadConfiguration(Path.of("src/main/resources/plugin.yml").toFile());

    @Test
    void everySubcommandHasANode() {
        assertEquals(Set.of("spawn", "list", "packs", "info", "reload", "validate", "skill", "give", "killall", "spawner", "debug", "stats"), BetterMobCommand.PERMISSIONS.keySet());
    }

    @Test
    void everyNodeIsDeclaredForOpsAndGrantedByAdmin() {
        for (String node : new TreeSet<>(BetterMobCommand.PERMISSIONS.values())) {
            assertEquals("op", PLUGIN.getString("permissions." + node + ".default"), node);
            assertTrue(PLUGIN.getBoolean("permissions.bettermob.admin.children." + node), node);
        }
    }

    @Test
    void adminGrantsNothingBeyondTheNodes() {
        Set<String> children = new TreeSet<>();
        for (String key : PLUGIN.getConfigurationSection("permissions.bettermob.admin.children").getKeys(true)) {
            if (PLUGIN.isBoolean("permissions.bettermob.admin.children." + key)) children.add(key);
        }
        assertEquals(new TreeSet<>(BetterMobCommand.PERMISSIONS.values()), children);
    }

    @Test
    void theCommandItselfNeedsNoPermission() {
        assertNull(PLUGIN.getString("commands.bettermob.permission"));
    }
}
