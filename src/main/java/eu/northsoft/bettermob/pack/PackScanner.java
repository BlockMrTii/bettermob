package eu.northsoft.bettermob.pack;

import eu.northsoft.bettermob.BetterMobPlugin;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public final class PackScanner {
    private final BetterMobPlugin plugin;
    private final File packsFolder;

    public PackScanner(BetterMobPlugin plugin) {
        this.plugin = plugin;
        this.packsFolder = new File(plugin.getDataFolder(), "packs");
    }

    public List<File> foldersFor(String name) {
        List<File> folders = new ArrayList<>();
        File flat = new File(plugin.getDataFolder(), name);
        if (flat.isDirectory()) folders.add(flat);

        if (!packsFolder.isDirectory()) {
            packsFolder.mkdirs();
            return folders;
        }
        List<String> disabled = plugin.getConfig().getStringList("DisabledPacks").stream()
                .map(value -> value.toLowerCase(Locale.ROOT)).toList();

        File[] packDirs = packsFolder.listFiles(File::isDirectory);
        if (packDirs == null) return folders;
        for (File packDir : packDirs) {
            if (disabled.contains(packDir.getName().toLowerCase(Locale.ROOT))) continue;
            File subfolder = findCaseInsensitive(packDir, name);
            if (subfolder != null) folders.add(subfolder);
        }
        return folders;
    }

    private File findCaseInsensitive(File parent, String name) {
        File[] children = parent.listFiles(File::isDirectory);
        if (children == null) return null;
        for (File child : children) if (child.getName().equalsIgnoreCase(name)) return child;
        return null;
    }

    public List<String> listPacks() {
        List<String> result = new ArrayList<>();
        File[] packDirs = packsFolder.listFiles(File::isDirectory);
        if (packDirs == null) return result;
        List<String> disabled = plugin.getConfig().getStringList("DisabledPacks").stream()
                .map(value -> value.toLowerCase(Locale.ROOT)).toList();
        for (File packDir : packDirs) {
            boolean enabled = !disabled.contains(packDir.getName().toLowerCase(Locale.ROOT));
            result.add(packDir.getName() + " " + plugin.messages().get(enabled ? "pack.enabled" : "pack.disabled"));
        }
        return result;
    }
}
