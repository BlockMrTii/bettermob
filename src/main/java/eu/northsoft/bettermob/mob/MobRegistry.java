package eu.northsoft.bettermob.mob;

import eu.northsoft.bettermob.BetterMobPlugin;
import eu.northsoft.bettermob.drop.DropEntry;
import eu.northsoft.bettermob.drop.DropTable;
import eu.northsoft.bettermob.pack.PackScanner;
import eu.northsoft.bettermob.pack.YamlFiles;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;
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

public final class MobRegistry {
    private final BetterMobPlugin plugin;
    private final File legacyFile;
    private final File folder;
    private final PackScanner packScanner;
    private final Map<String, MobDefinition> mobs = new LinkedHashMap<>();

    public MobRegistry(BetterMobPlugin plugin, PackScanner packScanner) {
        this.plugin = plugin;
        this.packScanner = packScanner;
        this.legacyFile = new File(plugin.getDataFolder(), "mobs.yml");
        this.folder = new File(plugin.getDataFolder(), "mobs");
    }

    public void load() {
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
                        plugin.messages().warn("mob.duplicate", "mob", id, "folder", sourceFolder.getPath());
                        continue;
                    }
                    MobDefinition definition = parse(id, entry.getValue());
                    if (definition != null) mobs.put(id.toLowerCase(Locale.ROOT), definition);
                }
            }
        }
        plugin.messages().info("mob.loaded", "count", mobs.size());
    }

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
                plugin.messages().warn("mob.migrationFailed", "mob", id, "error", exception.getMessage());
            }
        }
        File backup = new File(plugin.getDataFolder(), "mobs.yml.migrated");
        legacyFile.renameTo(backup);
        plugin.messages().info("mob.migrated");
    }

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

    public MobDefinition get(String id) {
        return mobs.get(id.toLowerCase(Locale.ROOT));
    }

    public Map<String, MobDefinition> all() {
        return mobs;
    }

    private MobDefinition parse(String id, ConfigurationSection section) {
        EntityType type;
        try {
            type = EntityType.valueOf(section.getString("Type", "ZOMBIE").toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException exception) {
            plugin.messages().warn("mob.unknownType", "mob", id, "type", section.getString("Type"));
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
                parseDrops(id, section.getStringList("Drops")),
                section.getString("Faction"),
                parseBossBar(id, modulesSection),
                List.copyOf(section.getStringList("Equipment")),
                parseSpawnRule(id, section.getConfigurationSection("Spawn"))
        );
    }

    private SpawnRule parseSpawnRule(String id, ConfigurationSection spawn) {
        if (spawn == null) return null;
        String timeName = spawn.getString("Time", "any");
        SpawnRule.Time time = enumOrNull(SpawnRule.Time.class, timeName);
        if (time == null) {
            plugin.messages().warn("mob.spawnTimeInvalid", "mob", id, "value", timeName);
            time = SpawnRule.Time.ANY;
        }
        return new SpawnRule(spawn.getStringList("Worlds"), spawn.getStringList("Biomes"), time, spawn.getDouble("Chance", 1));
    }

    private BossBarSettings parseBossBar(String id, ConfigurationSection modules) {
        if (modules == null) return null;
        ConfigurationSection bar = modules.getConfigurationSection("BossBar");
        if (bar == null) return modules.getBoolean("BossBar", false) ? BossBarSettings.defaults() : null;
        String colorName = bar.getString("Color", "RED");
        BarColor color = enumOrNull(BarColor.class, colorName);
        if (color == null) {
            plugin.messages().warn("mob.bossBarColorInvalid", "mob", id, "value", colorName);
            color = BarColor.RED;
        }
        String styleName = bar.getString("Style", "SOLID");
        BarStyle style = enumOrNull(BarStyle.class, styleName);
        if (style == null) {
            plugin.messages().warn("mob.bossBarStyleInvalid", "mob", id, "value", styleName);
            style = BarStyle.SOLID;
        }
        return new BossBarSettings(
                bar.getString("Title", BossBarSettings.DEFAULT_TITLE),
                Math.max(1, bar.getDouble("Range", BossBarSettings.DEFAULT_RANGE)),
                color,
                style,
                bar.getBoolean("CreateFog", false),
                bar.getBoolean("DarkenSky", false),
                bar.getBoolean("PlayMusic", false));
    }

    private static <E extends Enum<E>> E enumOrNull(Class<E> type, String value) {
        try {
            return Enum.valueOf(type, value.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException exception) {
            return null;
        }
    }

    private DropTable parseDrops(String id, List<String> lines) {
        List<DropEntry> entries = new ArrayList<>();
        for (String line : lines) {
            DropEntry entry = DropEntry.parse(line);
            if (entry == null) plugin.messages().warn("mob.dropLineInvalid", "mob", id, "line", line);
            else entries.add(entry);
        }
        return entries.isEmpty() ? null : DropTable.anonymous(entries);
    }

    private List<MobDefinition.SkillTrigger> parseSkillTriggers(String id, List<String> entries) {
        List<MobDefinition.SkillTrigger> triggers = new ArrayList<>();
        for (String entry : entries) {
            MobDefinition.SkillTrigger trigger = MobDefinition.SkillTrigger.parse(entry);
            if (trigger == null) plugin.messages().warn("mob.skillLineInvalid", "mob", id, "line", entry);
            else triggers.add(trigger);
        }
        return triggers;
    }

    private Map<DamageCause, Double> parseDamageModifiers(String id, List<String> entries) {
        Map<DamageCause, Double> modifiers = new EnumMap<>(DamageCause.class);
        for (String entry : entries) {
            String[] parts = entry.trim().split("\\s+");
            if (parts.length != 2) {
                plugin.messages().warn("mob.damageModifierInvalid", "mob", id, "entry", entry);
                continue;
            }
            DamageCause cause;
            try {
                cause = DamageCause.valueOf(parts[0].toUpperCase(Locale.ROOT));
            } catch (IllegalArgumentException exception) {
                plugin.messages().warn("mob.damageCauseUnknown", "mob", id, "cause", parts[0]);
                continue;
            }
            try {
                modifiers.put(cause, Double.parseDouble(parts[1]));
            } catch (NumberFormatException exception) {
                plugin.messages().warn("mob.multiplierInvalid", "mob", id, "value", parts[1]);
            }
        }
        return modifiers;
    }
}
