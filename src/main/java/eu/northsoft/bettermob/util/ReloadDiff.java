package eu.northsoft.bettermob.util;

import org.bukkit.configuration.ConfigurationSection;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeSet;

public record ReloadDiff(List<String> added, List<String> removed, List<String> changed) {
    public static int fingerprint(ConfigurationSection section) {
        return section.getValues(true).toString().hashCode();
    }

    public static ReloadDiff of(Map<String, Integer> before, Map<String, Integer> after) {
        List<String> added = new ArrayList<>();
        List<String> removed = new ArrayList<>();
        List<String> changed = new ArrayList<>();
        for (String id : new TreeSet<>(after.keySet())) {
            Integer old = before.get(id);
            if (old == null) added.add(id);
            else if (!old.equals(after.get(id))) changed.add(id);
        }
        for (String id : new TreeSet<>(before.keySet())) {
            if (!after.containsKey(id)) removed.add(id);
        }
        return new ReloadDiff(added, removed, changed);
    }

    public boolean isEmpty() {
        return added.isEmpty() && removed.isEmpty() && changed.isEmpty();
    }
}
