package eu.northsoft.bettermob.stats;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class KillStats {
    public record Row(UUID id, String name, int kills) {}

    private final Map<UUID, Map<String, Integer>> kills = new ConcurrentHashMap<>();
    private final Map<UUID, String> names = new ConcurrentHashMap<>();
    private volatile boolean dirty;
    private volatile boolean readOnly;

    public void record(UUID player, String name, String mobId) {
        kills.computeIfAbsent(player, id -> new ConcurrentHashMap<>()).merge(mobId.toLowerCase(Locale.ROOT), 1, Integer::sum);
        names.put(player, name);
        dirty = true;
    }

    public int total(UUID player) {
        Map<String, Integer> own = kills.get(player);
        return own == null ? 0 : own.values().stream().mapToInt(Integer::intValue).sum();
    }

    public int count(UUID player, String mobId) {
        Map<String, Integer> own = kills.get(player);
        return own == null ? 0 : own.getOrDefault(mobId.toLowerCase(Locale.ROOT), 0);
    }

    public List<Row> top(String mobId, int limit) {
        List<Row> rows = new ArrayList<>();
        for (UUID player : kills.keySet()) {
            int amount = mobId == null ? total(player) : count(player, mobId);
            if (amount > 0) rows.add(new Row(player, names.getOrDefault(player, player.toString()), amount));
        }
        rows.sort(Comparator.comparingInt(Row::kills).reversed().thenComparing(Row::name, String.CASE_INSENSITIVE_ORDER));
        return rows.size() > limit ? List.copyOf(rows.subList(0, Math.max(0, limit))) : List.copyOf(rows);
    }

    public boolean dirty() {
        return dirty;
    }

    public YamlConfiguration snapshot() {
        YamlConfiguration config = new YamlConfiguration();
        for (Map.Entry<UUID, Map<String, Integer>> entry : kills.entrySet()) {
            String path = "players." + entry.getKey();
            config.set(path + ".name", names.getOrDefault(entry.getKey(), entry.getKey().toString()));
            for (Map.Entry<String, Integer> mob : entry.getValue().entrySet()) config.set(path + ".kills." + mob.getKey(), mob.getValue());
        }
        return config;
    }

    public synchronized void flush(File file) throws IOException {
        if (!dirty || readOnly) return;
        dirty = false;
        try {
            YamlConfiguration snapshot = snapshot();
            File parent = file.getAbsoluteFile().getParentFile();
            if (parent != null) parent.mkdirs();
            File temp = new File(parent, file.getName() + ".tmp");
            snapshot.save(temp);
            try {
                Files.move(temp.toPath(), file.toPath(), StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
            } catch (AtomicMoveNotSupportedException exception) {
                Files.move(temp.toPath(), file.toPath(), StandardCopyOption.REPLACE_EXISTING);
            }
        } catch (IOException | RuntimeException exception) {
            dirty = true;
            throw exception;
        }
    }

    public void protect() {
        readOnly = true;
    }

    public void load(YamlConfiguration config) {
        kills.clear();
        names.clear();
        ConfigurationSection players = config.getConfigurationSection("players");
        if (players == null) return;
        for (String key : players.getKeys(false)) {
            UUID id;
            try {
                id = UUID.fromString(key);
            } catch (IllegalArgumentException exception) {
                continue;
            }
            ConfigurationSection section = players.getConfigurationSection(key);
            if (section == null) continue;
            names.put(id, section.getString("name", key));
            ConfigurationSection own = section.getConfigurationSection("kills");
            if (own == null) continue;
            Map<String, Integer> map = new ConcurrentHashMap<>();
            for (String mob : own.getKeys(false)) {
                int amount = own.getInt(mob);
                if (amount > 0) map.put(mob.toLowerCase(Locale.ROOT), amount);
            }
            if (!map.isEmpty()) kills.put(id, map);
        }
        dirty = false;
    }
}
