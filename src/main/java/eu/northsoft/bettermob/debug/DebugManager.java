package eu.northsoft.bettermob.debug;

import eu.northsoft.bettermob.BetterMobPlugin;
import eu.northsoft.bettermob.util.Tasks;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class DebugManager {
    public enum Level { OFF, INFO, VERBOSE }

    private static final String WATCH_PERMISSION = "bettermob.debug";

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

    private record Session(UUID player, String mob, UUID entity, long expiresAt) {}

    private static final long SESSION_MILLIS = 5 * 60 * 1000L;

    private final Map<UUID, Session> sessions = new ConcurrentHashMap<>();

    public boolean info() {
        return level != Level.OFF || hasSessions();
    }

    public boolean verbose() {
        return level == Level.VERBOSE || hasSessions();
    }

    public long startSession(Player player, String mobId, UUID entity) {
        long expires = System.currentTimeMillis() + SESSION_MILLIS;
        sessions.put(player.getUniqueId(), new Session(player.getUniqueId(), mobId.toLowerCase(Locale.ROOT), entity, expires));
        return SESSION_MILLIS / 60000L;
    }

    public boolean endSession(UUID player) {
        return sessions.remove(player) != null;
    }

    public void endSessionsOf(UUID entity) {
        if (!sessions.isEmpty()) sessions.values().removeIf(session -> session.entity().equals(entity));
    }

    private static final ThreadLocal<UUID> SOURCE = new ThreadLocal<>();
    private static final Runnable NO_SCOPE = () -> { };

    public Runnable enter(UUID source) {
        if (sessions.isEmpty()) return NO_SCOPE;
        UUID previous = SOURCE.get();
        SOURCE.set(source);
        return () -> {
            if (previous == null) SOURCE.remove();
            else SOURCE.set(previous);
        };
    }

    private boolean hasSessions() {
        if (sessions.isEmpty()) return false;
        long now = System.currentTimeMillis();
        sessions.values().removeIf(session -> session.expiresAt() <= now);
        return !sessions.isEmpty();
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
        if (level != Level.OFF && matches(subjects)) emit(message);
        emitToSessions(message);
    }

    public void verbose(String message, String... subjects) {
        if (level == Level.VERBOSE && matches(subjects)) emit(message);
        emitToSessions(message);
    }

    public void randomSpawn(UUID playerId, String mobId, String message) {
        if (!matches(new String[]{mobId})) return;
        if (level == Level.OFF) {
            if (watchers.contains(playerId)) {
                Player player = Bukkit.getPlayer(playerId);
                if (player != null) Tasks.runOn(plugin, player, () -> {
                    if (player.isOnline() && player.hasPermission(WATCH_PERMISSION)) {
                        player.sendMessage("§8[§7debug§8] §7" + message);
                    }
                });
            }
            return;
        }
        info(message, mobId);
    }

    private void emitToSessions(String message) {
        if (sessions.isEmpty()) return;
        UUID source = SOURCE.get();
        if (source == null) return;
        long now = System.currentTimeMillis();
        for (Session session : sessions.values()) {
            if (session.expiresAt() <= now) {
                sessions.remove(session.player(), session);
                continue;
            }
            if (!session.entity().equals(source)) continue;
            Player player = Bukkit.getPlayer(session.player());
            if (player == null) {
                sessions.remove(session.player(), session);
                continue;
            }
            Tasks.runOn(plugin, player, () -> {
                if (sessions.get(session.player()) == session) player.sendMessage("§8[§7test§8] §7" + message);
            });
        }
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
            else Tasks.runOn(plugin, player, () -> {
                if (player.isOnline() && player.hasPermission(WATCH_PERMISSION)) {
                    player.sendMessage("§8[§7debug§8] §7" + message);
                } else watchers.remove(id);
            });
        }
    }
}
