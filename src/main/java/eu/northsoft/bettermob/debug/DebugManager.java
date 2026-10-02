package eu.northsoft.bettermob.debug;

import eu.northsoft.bettermob.BetterMobPlugin;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.Locale;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class DebugManager {
    public enum Level { OFF, INFO, VERBOSE }

    private final BetterMobPlugin plugin;
    private final Set<String> filters = ConcurrentHashMap.newKeySet();
    private final Set<UUID> watchers = ConcurrentHashMap.newKeySet();
    private volatile Level level = Level.OFF;

    public DebugManager(BetterMobPlugin plugin) {
        this.plugin = plugin;
        reload();
    }

    public void reload() {
        String configured = String.valueOf(plugin.getConfig().get("Debug", "off")).toLowerCase(Locale.ROOT);
        level = switch (configured) {
            case "true", "on", "info" -> Level.INFO;
            case "verbose" -> Level.VERBOSE;
            default -> Level.OFF;
        };
    }

    public boolean info() {
        return level != Level.OFF;
    }

    public boolean verbose() {
        return level == Level.VERBOSE;
    }

    public Level level() {
        return level;
    }

    public void level(Level level) {
        this.level = level;
    }

    public Set<String> filters() {
        return filters;
    }

    public boolean toggleWatcher(Player player) {
        if (watchers.remove(player.getUniqueId())) return false;
        watchers.add(player.getUniqueId());
        return true;
    }

    public void info(String message, String... subjects) {
        if (info() && matches(subjects)) emit(message);
    }

    public void verbose(String message, String... subjects) {
        if (verbose() && matches(subjects)) emit(message);
    }

    private boolean matches(String[] subjects) {
        if (filters.isEmpty()) return true;
        for (String subject : subjects) {
            if (subject != null && filters.contains(subject.toLowerCase(Locale.ROOT))) return true;
        }
        return false;
    }

    private void emit(String message) {
        plugin.getLogger().info("[debug] " + message);
        for (UUID id : watchers) {
            Player player = Bukkit.getPlayer(id);
            if (player == null) watchers.remove(id);
            else player.sendMessage("§8[§7debug§8] §7" + message);
        }
    }
}
