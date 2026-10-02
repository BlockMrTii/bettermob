package eu.northsoft.bettermob;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.components.CustomModelDataComponent;
import org.bukkit.persistence.PersistentDataType;

import java.io.File;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * Laedt items/**.yml (plus items/ jedes Packs) - jeder Top-Level-Key mit einem "Id:" ist ein
 * Item. Wie bei MythicMobs ist "Id" das Vanilla-Material und "Model" die CustomModelData,
 * ueber die das Resourcepack das Aussehen waehlt. Gebaute Items tragen ihre ID im PDC, damit
 * ~onUse und takeitem sie wiedererkennen.
 */
final class ItemRegistry {
    private final BetterMobPlugin plugin;
    private final PackScanner packScanner;
    private final File folder;
    private final NamespacedKey itemIdKey;
    private final Map<String, ItemDefinition> items = new LinkedHashMap<>();

    ItemRegistry(BetterMobPlugin plugin, PackScanner packScanner) {
        this.plugin = plugin;
        this.packScanner = packScanner;
        this.folder = new File(plugin.getDataFolder(), "items");
        this.itemIdKey = new NamespacedKey(plugin, "item_id");
    }

    void load() {
        folder.mkdirs();
        items.clear();
        for (File sourceFolder : packScanner.foldersFor("items")) {
            for (File file : YamlFiles.collect(sourceFolder)) {
                YamlConfiguration config = YamlConfiguration.loadConfiguration(file);
                for (String id : config.getKeys(false)) {
                    ConfigurationSection section = config.getConfigurationSection(id);
                    if (section == null || !section.contains("Id")) continue;
                    if (items.containsKey(id.toLowerCase(Locale.ROOT))) {
                        plugin.getLogger().warning("Item '" + id + "' aus " + sourceFolder.getPath() + " ueberschreibt eine bereits geladene Definition - ignoriert.");
                        continue;
                    }
                    ItemDefinition definition = parse(id, section);
                    if (definition != null) items.put(id.toLowerCase(Locale.ROOT), definition);
                }
            }
        }
        plugin.getLogger().info(items.size() + " Items geladen.");
    }

    ItemDefinition get(String id) {
        return id == null ? null : items.get(id.toLowerCase(Locale.ROOT));
    }

    Set<String> ids() {
        return items.keySet();
    }

    /** Definition hinter einem gebauten Item, oder null wenn es keins von uns ist. */
    ItemDefinition definitionOf(ItemStack stack) {
        String id = idOf(stack);
        return id == null ? null : get(id);
    }

    String idOf(ItemStack stack) {
        if (stack == null || !stack.hasItemMeta()) return null;
        return stack.getItemMeta().getPersistentDataContainer().get(itemIdKey, PersistentDataType.STRING);
    }

    ItemStack create(ItemDefinition definition, int amount) {
        ItemStack stack = new ItemStack(definition.material, Math.max(1, amount));
        ItemMeta meta = stack.getItemMeta();
        meta.displayName(text(definition.displayName));
        if (!definition.lore.isEmpty()) meta.lore(definition.lore.stream().map(this::text).toList());
        if (definition.model >= 0) {
            CustomModelDataComponent modelData = meta.getCustomModelDataComponent();
            modelData.setFloats(List.of((float) definition.model));
            meta.setCustomModelDataComponent(modelData);
        }
        meta.getPersistentDataContainer().set(itemIdKey, PersistentDataType.STRING, definition.id);
        stack.setItemMeta(meta);
        return stack;
    }

    /** Ohne das Ausschalten von Kursiv zeigt der Client benannte Items schraeg an. */
    private Component text(String raw) {
        return LegacyComponentSerializer.legacyAmpersand().deserialize(raw).decoration(TextDecoration.ITALIC, false);
    }

    private ItemDefinition parse(String id, ConfigurationSection section) {
        Material material = Material.matchMaterial(section.getString("Id", ""));
        if (material == null) {
            plugin.getLogger().warning("Item '" + id + "': unbekanntes Material '" + section.getString("Id") + "'.");
            return null;
        }
        List<MobDefinition.SkillTrigger> triggers = new ArrayList<>();
        for (String line : section.getStringList("Skills")) {
            MobDefinition.SkillTrigger trigger = MobDefinition.SkillTrigger.parse(line);
            if (trigger == null) plugin.getLogger().warning("Item '" + id + "': Skill-Zeile '" + line + "' konnte nicht geparst werden.");
            else triggers.add(trigger);
        }
        return new ItemDefinition(id, material, section.getInt("Model", -1), section.getString("Display", id),
                List.copyOf(section.getStringList("Lore")), List.copyOf(triggers));
    }
}
