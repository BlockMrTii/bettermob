package eu.northsoft.bettermob.util;

import org.bukkit.configuration.ConfigurationSection;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import java.util.TreeSet;

public record ReloadDiff(List<String> added, List<String> removed, List<String> changed) {
    public static String fingerprint(ConfigurationSection section) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256").digest(section.getValues(true).toString().getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException(exception);
        }
    }

    public static ReloadDiff of(Map<String, String> before, Map<String, String> after) {
        List<String> added = new ArrayList<>();
        List<String> removed = new ArrayList<>();
        List<String> changed = new ArrayList<>();
        for (String id : new TreeSet<>(after.keySet())) {
            String old = before.get(id);
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
