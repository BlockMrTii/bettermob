package eu.northsoft.bettermob;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Sammelt alle .yml-Dateien unter einem Ordner, beliebig tief verschachtelt. Grosse Packs
 * (z.B. nogs_menagerie) legen die meisten Mobs/Skills in Unterordnern ab wie
 * Mobs/npcs/, Mobs/companions/, Skills/cards/ - eine flache Ordnerliste wuerde davon
 * fast alles uebersehen.
 */
final class YamlFiles {
    private YamlFiles() {}

    static List<File> collect(File folder) {
        List<File> result = new ArrayList<>();
        walk(folder, result);
        return result;
    }

    private static void walk(File folder, List<File> result) {
        File[] children = folder.listFiles();
        if (children == null) return;
        for (File child : children) {
            if (child.isDirectory()) walk(child, result);
            else if (child.getName().toLowerCase(Locale.ROOT).endsWith(".yml")) result.add(child);
        }
    }
}
