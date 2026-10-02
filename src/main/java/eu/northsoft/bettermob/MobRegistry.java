package eu.northsoft.bettermob;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.EntityType;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Laedt mobs/*.yml - eine Datei pro Mob, Dateiname = Mob-ID, wie bei MysticMobs' Mobs-Ordner.
 * Zusaetzlich wird jeder Unterordner in packs/ mit einem eigenen mobs/-Ordner mitgeladen -
 * wie MysticMobs' Mob-Packs (siehe PackScanner).
 */
final class MobRegistry {
    private final BetterMobPlugin plugin;
    private final File legacyFile;
    private final File folder;
    private final PackScanner packScanner;
    private final Map<String, MobDefinition> mobs = new LinkedHashMap<>();

    MobRegistry(BetterMobPlugin plugin, PackScanner packScanner) {
        this.plugin = plugin;
        this.packScanner = packScanner;
        this.legacyFile = new File(plugin.getDataFolder(), "mobs.yml");
        this.folder = new File(plugin.getDataFolder(), "mobs");
    }

    void load() {
        migrateLegacyFile();
        if (!folder.exists()) {
            folder.mkdirs();
            plugin.saveResource("mobs/skeleton_knight.yml", false);
            plugin.saveResource("mobs/zombie_brute.yml", false);
            plugin.saveResource("mobs/nm_wumpus.yml", false);
        }

        mobs.clear();
        for (File sourceFolder : packScanner.foldersFor("mobs")) {
            for (File file : YamlFiles.collect(sourceFolder)) {
                for (Map.Entry<String, ConfigurationSection> entry : extractMobSections(file).entrySet()) {
                    String id = entry.getKey();
                    if (mobs.containsKey(id.toLowerCase(Locale.ROOT))) {
                        plugin.getLogger().warning("Mob '" + id + "' aus " + sourceFolder.getPath() + " ueberschreibt eine bereits geladene Definition - ignoriert.");
                        continue;
                    }
                    MobDefinition definition = parse(id, entry.getValue());
                    if (definition != null) mobs.put(id.toLowerCase(Locale.ROOT), definition);
                }
            }
        }
        plugin.getLogger().info(mobs.size() + " Mobs geladen.");
    }

    /** Alte einzelne mobs.yml automatisch in je eine Datei pro Mob aufteilen - kein Neuschreiben noetig. */
    private void migrateLegacyFile() {
        if (!legacyFile.exists()) return;
        folder.mkdirs();
        YamlConfiguration legacy = YamlConfiguration.loadConfiguration(legacyFile);
        for (String id : legacy.getKeys(false)) {
            ConfigurationSection section = legacy.getConfigurationSection(id);
            if (section == null) continue;
            File target = new File(folder, id.toLowerCase(Locale.ROOT) + ".yml");
            if (target.exists()) continue;
            YamlConfiguration single = new YamlConfiguration();
            for (String key : section.getKeys(false)) single.set(key, section.get(key));
            try {
                single.save(target);
            } catch (IOException exception) {
                plugin.getLogger().warning("Migration von '" + id + "' fehlgeschlagen: " + exception.getMessage());
            }
        }
        File backup = new File(plugin.getDataFolder(), "mobs.yml.migrated");
        legacyFile.renameTo(backup);
        plugin.getLogger().info("mobs.yml automatisch nach mobs/ migriert (Sicherung: mobs.yml.migrated).");
    }

    /**
     * Eine Datei kann entweder ein einzelner Mob sein (Type/Display/etc. direkt auf
     * Dateiebene, ID = Dateiname) oder mehrere Mobs enthalten (jeder Top-Level-Key ist
     * eine eigene Mob-ID mit eigenem Type/Display/etc. darunter - wie bei MysticMobs'
     * klassischer mobs.yml). Eine Datei mit genau einem Top-Level-Key ohne "Type" auf
     * Dateiebene wird als versehentlich falsch eingerueckter Einzel-Mob behandelt und
     * automatisch ausgewickelt, statt stumm auf ZOMBIE/Default-Werte zurueckzufallen.
     */
    private Map<String, ConfigurationSection> extractMobSections(File file) {
        YamlConfiguration root = YamlConfiguration.loadConfiguration(file);
        String fileId = file.getName().substring(0, file.getName().length() - 4);
        if (root.contains("Type")) return Map.of(fileId, root);

        List<String> keys = List.copyOf(root.getKeys(false));
        if (keys.size() == 1) {
            ConfigurationSection nested = root.getConfigurationSection(keys.get(0));
            return Map.of(fileId, nested != null ? nested : root);
        }

        Map<String, ConfigurationSection> result = new LinkedHashMap<>();
        for (String key : keys) {
            ConfigurationSection nested = root.getConfigurationSection(key);
            if (nested != null) result.put(key, nested);
        }
        return result;
    }

    MobDefinition get(String id) {
        return mobs.get(id.toLowerCase(Locale.ROOT));
    }

    Map<String, MobDefinition> all() {
        return mobs;
    }

    private MobDefinition parse(String id, ConfigurationSection section) {
        EntityType type;
        try {
            type = EntityType.valueOf(section.getString("Type", "ZOMBIE").toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException exception) {
            plugin.getLogger().warning("Mob '" + id + "': unbekannter Type '" + section.getString("Type") + "'.");
            return null;
        }
        ConfigurationSection optionsSection = section.getConfigurationSection("Options");
        MobDefinition.Options options = optionsSection == null ? MobDefinition.Options.DEFAULT : new MobDefinition.Options(
                optionsSection.getBoolean("Collidable", true),
                optionsSection.contains("MovementSpeed") ? optionsSection.getDouble("MovementSpeed") : -1,
                optionsSection.getBoolean("PreventOtherDrops", false),
                optionsSection.getBoolean("Silent", false),
                optionsSection.getBoolean("PreventRenaming", false),
                optionsSection.getBoolean("PreventLeashing", false),
                optionsSection.getBoolean("AlwaysShowName", false),
                optionsSection.getBoolean("PreventSunburn", true),
                optionsSection.getBoolean("Invincible", false),
                optionsSection.getBoolean("Invisible", false),
                optionsSection.getBoolean("CanMove", true),
                optionsSection.getBoolean("Interactable", true),
                optionsSection.getBoolean("Marker", false),
                optionsSection.getString("ItemHead"),
                optionsSection.contains("KnockbackResistance") ? optionsSection.getDouble("KnockbackResistance") : -1,
                optionsSection.contains("FollowRange") ? optionsSection.getDouble("FollowRange") : -1,
                optionsSection.getBoolean("PreventItemPickup", false),
                optionsSection.contains("Scale") ? optionsSection.getDouble("Scale") : -1
        );
        ConfigurationSection modulesSection = section.getConfigurationSection("Modules");
        boolean threatTable = modulesSection != null && modulesSection.getBoolean("ThreatTable", false);

        return new MobDefinition(
                id,
                type,
                section.getString("Display", "&f" + id),
                section.getString("Model", id),
                section.getDouble("Health", 20.0),
                section.getDouble("Damage", 2.0),
                section.getBoolean("RemoveAi", false),
                List.copyOf(section.getStringList("AIGoalSelectors")),
                List.copyOf(section.getStringList("AITargetSelectors")),
                options,
                threatTable,
                parseDamageModifiers(id, section.getStringList("DamageModifiers")),
                parseSkillTriggers(id, section.getStringList("Skills")),
                parseDrops(id, section.getStringList("Drops"))
        );
    }

    /** "Drops:" eines Mobs: Zeilen wie in einer Drop-Table (auch Verweise auf Tabellen). Null, wenn leer. */
    private DropTable parseDrops(String id, List<String> lines) {
        List<DropEntry> entries = new ArrayList<>();
        for (String line : lines) {
            DropEntry entry = DropEntry.parse(line);
            if (entry == null) plugin.getLogger().warning("Mob '" + id + "': Drop-Zeile '" + line + "' konnte nicht geparst werden.");
            else entries.add(entry);
        }
        return entries.isEmpty() ? null : DropTable.anonymous(entries);
    }

    /** "skill{s=xyz} ~onInteract" - ungueltige Zeilen werden uebersprungen und geloggt. */
    private List<MobDefinition.SkillTrigger> parseSkillTriggers(String id, List<String> entries) {
        List<MobDefinition.SkillTrigger> triggers = new ArrayList<>();
        for (String entry : entries) {
            MobDefinition.SkillTrigger trigger = MobDefinition.SkillTrigger.parse(entry);
            if (trigger == null) plugin.getLogger().warning("Mob '" + id + "': Skill-Zeile '" + entry + "' konnte nicht geparst werden.");
            else triggers.add(trigger);
        }
        return triggers;
    }

    /** "FIRE 1.2" -> Schaden dieser Ursache wird beim Mob mit 1.2 multipliziert. */
    private Map<DamageCause, Double> parseDamageModifiers(String id, List<String> entries) {
        Map<DamageCause, Double> modifiers = new EnumMap<>(DamageCause.class);
        for (String entry : entries) {
            String[] parts = entry.trim().split("\\s+");
            if (parts.length != 2) {
                plugin.getLogger().warning("Mob '" + id + "': ungueltiger DamageModifier-Eintrag '" + entry + "'.");
                continue;
            }
            DamageCause cause;
            try {
                cause = DamageCause.valueOf(parts[0].toUpperCase(Locale.ROOT));
            } catch (IllegalArgumentException exception) {
                plugin.getLogger().warning("Mob '" + id + "': unbekannte DamageCause '" + parts[0] + "'.");
                continue;
            }
            try {
                modifiers.put(cause, Double.parseDouble(parts[1]));
            } catch (NumberFormatException exception) {
                plugin.getLogger().warning("Mob '" + id + "': ungueltiger Multiplikator '" + parts[1] + "'.");
            }
        }
        return modifiers;
    }
}
