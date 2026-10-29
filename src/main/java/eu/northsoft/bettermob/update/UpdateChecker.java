package eu.northsoft.bettermob.update;

import eu.northsoft.bettermob.BetterMobPlugin;
import eu.northsoft.bettermob.util.Tasks;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class UpdateChecker implements Listener {
    public record Release(String version, String url) {}

    private static final String LATEST = "https://api.github.com/repos/HyperGaming99/bettermob/releases/latest";
    private static final long INTERVAL_TICKS = 6L * 60 * 60 * 20;
    private static final long JOIN_DELAY_TICKS = 60L;
    private static final Pattern TAG = Pattern.compile("\"tag_name\"\\s*:\\s*\"([^\"]{1,40})\"");
    private static final Pattern URL = Pattern.compile("\"html_url\"\\s*:\\s*\"(https://github\\.com/[^\"]{1,100}/releases/[^\"]{1,100})\"");

    private final BetterMobPlugin plugin;
    private final HttpClient client = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).build();
    private volatile Release available;
    private volatile String announced;
    private Runnable cancel = () -> { };

    public UpdateChecker(BetterMobPlugin plugin) {
        this.plugin = plugin;
    }

    public void start() {
        stop();
        cancel = Tasks.runGlobalTimer(plugin, INTERVAL_TICKS, () -> Tasks.runAsync(plugin, this::check));
        Tasks.runAsync(plugin, this::check);
    }

    public void stop() {
        cancel.run();
        cancel = () -> { };
    }

    public Release available() {
        return available;
    }

    void check() {
        try {
            HttpRequest request = HttpRequest.newBuilder(URI.create(LATEST))
                    .timeout(Duration.ofSeconds(10))
                    .header("Accept", "application/vnd.github+json")
                    .header("User-Agent", "BetterMob/" + plugin.getPluginMeta().getVersion())
                    .GET().build();
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() != 200) return;
            Release latest = parse(response.body());
            String current = plugin.getPluginMeta().getVersion();
            if (latest == null || !isNewer(current, latest.version())) {
                available = null;
                return;
            }
            available = latest;
            if (!latest.version().equals(announced)) {
                announced = latest.version();
                plugin.messages().warn("update.available", "current", current, "latest", latest.version(), "url", latest.url());
            }
        } catch (IOException | RuntimeException exception) {
            plugin.debug().info("update check failed: " + exception.getMessage());
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
        }
    }

    static Release parse(String body) {
        Matcher tag = TAG.matcher(body);
        if (!tag.find()) return null;
        Matcher url = URL.matcher(body);
        String version = tag.group(1).startsWith("v") ? tag.group(1).substring(1) : tag.group(1);
        return new Release(version, url.find() ? url.group(1) : "https://github.com/HyperGaming99/bettermob/releases/latest");
    }

    public static boolean isNewer(String current, String latest) {
        int[] now = numbers(current);
        int[] next = numbers(latest);
        for (int i = 0; i < Math.max(now.length, next.length); i++) {
            int a = i < now.length ? now[i] : 0;
            int b = i < next.length ? next[i] : 0;
            if (a != b) return b > a;
        }
        return false;
    }

    private static int[] numbers(String version) {
        String text = version.startsWith("v") ? version.substring(1) : version;
        int end = 0;
        while (end < text.length() && (Character.isDigit(text.charAt(end)) || text.charAt(end) == '.')) end++;
        String[] parts = text.substring(0, end).split("\\.");
        int[] result = new int[parts.length];
        for (int i = 0; i < parts.length; i++) {
            try {
                result[i] = parts[i].isEmpty() ? 0 : Integer.parseInt(parts[i]);
            } catch (NumberFormatException exception) {
                result[i] = 0;
            }
        }
        return result;
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onJoin(PlayerJoinEvent event) {
        Release release = available;
        Player player = event.getPlayer();
        if (release == null || !player.hasPermission("bettermob.admin")) return;
        Tasks.runLater(plugin, player, JOIN_DELAY_TICKS, () -> plugin.messages().send(player, "update.available",
                "current", plugin.getPluginMeta().getVersion(), "latest", release.version(), "url", release.url()));
    }
}
