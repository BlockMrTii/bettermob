package eu.northsoft.bettermob.drop;

import eu.northsoft.bettermob.BetterMobPlugin;
import eu.northsoft.bettermob.item.ItemDefinition;
import eu.northsoft.bettermob.item.ItemRegistry;
import eu.northsoft.bettermob.pack.PackScanner;
import eu.northsoft.bettermob.pack.YamlFiles;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.inventory.ItemStack;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;

public final class DropRegistry {
    private static final int MAX_DEPTH = 5;

    public record Result(List<ItemStack> items, int exp) {}

    private final BetterMobPlugin plugin;
    private final PackScanner packScanner;
    private final ItemRegistry items;
    private final File folder;
    private volatile Map<String, DropTable> tables = Map.of();
    private final Set<String> warned = Collections.synchronizedSet(new HashSet<>());

    public DropRegistry(BetterMobPlugin plugin, PackScanner packScanner, ItemRegistry items) {
        this.plugin = plugin;
        this.packScanner = packScanner;
        this.items = items;
        this.folder = new File(plugin.getDataFolder(), "droptables");
    }

    public void load() {
        folder.mkdirs();
        Map<String, DropTable> loaded = new LinkedHashMap<>();
        warned.clear();
        for (File sourceFolder : packScanner.foldersFor("droptables")) {
            for (File file : YamlFiles.collect(sourceFolder)) {
                YamlConfiguration config = YamlConfiguration.loadConfiguration(file);
                for (String id : config.getKeys(false)) {
                    ConfigurationSection section = config.getConfigurationSection(id);
                    if (section == null || !section.isList("Drops")) continue;
                    if (loaded.containsKey(id.toLowerCase(Locale.ROOT))) {
                        plugin.messages().warn("drop.duplicate", "table", id, "folder", sourceFolder.getPath());
                        continue;
                    }
                    loaded.put(id.toLowerCase(Locale.ROOT), new DropTable(id,
                            Math.max(0, section.getInt("MinItems", 0)),
                            section.getInt("MaxItems", Integer.MAX_VALUE),
                            parseLines(id, section.getStringList("Drops"))));
                }
            }
        }
        tables = loaded;
        plugin.messages().info("drop.loaded", "count", loaded.size());
    }

    private List<DropEntry> parseLines(String owner, List<String> lines) {
        List<DropEntry> entries = new ArrayList<>();
        for (String line : lines) {
            DropEntry entry = DropEntry.parse(line);
            if (entry == null) plugin.messages().warn("drop.lineInvalid", "table", owner, "line", line);
            else entries.add(entry);
        }
        return entries;
    }

    public DropTable get(String id) {
        return id == null ? null : tables.get(id.toLowerCase(Locale.ROOT));
    }

    public Set<String> ids() {
        return tables.keySet();
    }

    public Result roll(DropTable table) {
        List<ItemStack> stacks = new ArrayList<>();
        int exp = roll(table, stacks, 0);
        return new Result(stacks, exp);
    }

    private int roll(DropTable table, List<ItemStack> out, int depth) {
        ThreadLocalRandom random = ThreadLocalRandom.current();
        int exp = 0;
        List<DropEntry> hit = new ArrayList<>();
        List<DropEntry> missed = new ArrayList<>();
        for (DropEntry entry : table.entries()) {
            boolean passed = random.nextDouble() < entry.chance();
            if (entry.isExp()) {
                if (passed) exp += amount(entry);
            } else {
                (passed ? hit : missed).add(entry);
            }
        }

        Collections.shuffle(hit, random);
        while (hit.size() > table.maxItems()) hit.remove(hit.size() - 1);
        Collections.shuffle(missed, random);
        while (hit.size() < table.minItems() && !missed.isEmpty()) hit.add(missed.remove(missed.size() - 1));

        for (DropEntry entry : hit) exp += apply(entry, out, depth);
        return exp;
    }

    private int apply(DropEntry entry, List<ItemStack> out, int depth) {
        int amount = amount(entry);
        DropTable nested = get(entry.name());
        if (nested != null) {
            if (depth >= MAX_DEPTH) {
                warnOnce("drop.tooDeep", "table", entry.name());
                return 0;
            }
            int exp = 0;
            for (int i = 0; i < Math.max(1, amount); i++) exp += roll(nested, out, depth + 1);
            return exp;
        }
        ItemDefinition item = items.get(entry.name());
        if (item != null) {
            addStacks(out, items.create(item, 1), amount);
            return 0;
        }
        Material material = Material.matchMaterial(entry.name());
        if (material != null && material.isItem()) {
            addStacks(out, new ItemStack(material), amount);
            return 0;
        }
        warnOnce("drop.unresolvable", "drop", entry.name());
        return 0;
    }

    private static int amount(DropEntry entry) {
        return entry.min() >= entry.max() ? entry.min() : ThreadLocalRandom.current().nextInt(entry.min(), entry.max() + 1);
    }

    private static void addStacks(List<ItemStack> out, ItemStack template, int amount) {
        int stackSize = Math.max(1, template.getMaxStackSize());
        while (amount > 0) {
            ItemStack stack = template.clone();
            stack.setAmount(Math.min(stackSize, amount));
            out.add(stack);
            amount -= stack.getAmount();
        }
    }

    private void warnOnce(String key, Object... placeholders) {
        if (warned.add(plugin.messages().get(key, placeholders))) plugin.messages().warn(key, placeholders);
    }
}
