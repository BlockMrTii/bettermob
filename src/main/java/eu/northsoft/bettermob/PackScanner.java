package eu.northsoft.bettermob;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Packs wie bei MysticMobs: jeder Unterordner von packs/ ist ein eigenstaendiges Bundle
 * mit eigenem mobs/- und skills/-Ordner. Die flachen mobs/- und skills/-Ordner im
 * Plugin-Root bleiben weiterhin der "Default"-Pack - kein Bruch fuer bestehende Setups.
 */
final class PackScanner {
    private final BetterMobPlugin plugin;
    private final File packsFolder;

    PackScanner(BetterMobPlugin plugin) {
        this.plugin = plugin;
        this.packsFolder = new File(plugin.getDataFolder(), "packs");
    }

    /** Alle existierenden Ordner mit dem Namen "name" - der flache Default-Ordner zuerst, dann jeder aktivierte Pack. */
    List<File> foldersFor(String name) {
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

    /** Linux ist case-sensitiv, MysticMobs-Packs nennen ihre Ordner aber ueblicherweise
     *  "Mobs"/"Skills" (gross) - sonst findet die Suche den Ordner nicht und der Pack
     *  wirkt "aktiv", liefert aber trotzdem keine Mobs/Skills. */
    private File findCaseInsensitive(File parent, String name) {
        File[] children = parent.listFiles(File::isDirectory);
        if (children == null) return null;
        for (File child : children) if (child.getName().equalsIgnoreCase(name)) return child;
        return null;
    }

    /** Fuer /bettermob packs - Name + ob aktiv. */
    List<String> listPacks() {
        List<String> result = new ArrayList<>();
        File[] packDirs = packsFolder.listFiles(File::isDirectory);
        if (packDirs == null) return result;
        List<String> disabled = plugin.getConfig().getStringList("DisabledPacks").stream()
                .map(value -> value.toLowerCase(Locale.ROOT)).toList();
        for (File packDir : packDirs) {
            boolean enabled = !disabled.contains(packDir.getName().toLowerCase(Locale.ROOT));
            result.add(packDir.getName() + (enabled ? " §a(aktiv)" : " §c(deaktiviert)"));
        }
        return result;
    }
}
