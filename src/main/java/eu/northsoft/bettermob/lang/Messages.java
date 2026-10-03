package eu.northsoft.bettermob.lang;

import eu.northsoft.bettermob.BetterMobPlugin;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Locale;

public final class Messages {
    private static final List<String> BUNDLED = List.of("en", "de");

    private final BetterMobPlugin plugin;
    private final File folder;
    private YamlConfiguration english;
    private YamlConfiguration selected;

    public Messages(BetterMobPlugin plugin) {
        this.plugin = plugin;
        this.folder = new File(plugin.getDataFolder(), "lang");
        reload();
    }

    public void reload() {
        folder.mkdirs();
        for (String code : BUNDLED) {
            if (!new File(folder, code + ".yml").exists()) plugin.saveResource("lang/" + code + ".yml", false);
        }
        english = loadBundledEnglish();
        String language = plugin.getConfig().getString("Language", "en").toLowerCase(Locale.ROOT);
        File file = new File(folder, language + ".yml");
        if (file.isFile()) {
            selected = YamlConfiguration.loadConfiguration(file);
        } else {
            selected = english;
            warn("lang.missing", "language", language);
        }
    }

    public String get(String key, Object... placeholders) {
        String text = selected.getString(key, english.getString(key, key)).replace('&', '§');
        for (int i = 0; i + 1 < placeholders.length; i += 2) {
            text = text.replace("{" + placeholders[i] + "}", String.valueOf(placeholders[i + 1]));
        }
        return text;
    }

    public void send(CommandSender sender, String key, Object... placeholders) {
        sender.sendMessage(get(key, placeholders));
    }

    public void info(String key, Object... placeholders) {
        plugin.getLogger().info(plain(get(key, placeholders)));
    }

    public void warn(String key, Object... placeholders) {
        plugin.getLogger().warning(plain(get(key, placeholders)));
    }

    public void severe(String key, Object... placeholders) {
        plugin.getLogger().severe(plain(get(key, placeholders)));
    }

    private static String plain(String text) {
        return text.replaceAll("§[0-9a-fk-or]", "");
    }

    private YamlConfiguration loadBundledEnglish() {
        try (InputStream stream = plugin.getResource("lang/en.yml")) {
            return YamlConfiguration.loadConfiguration(new InputStreamReader(stream, StandardCharsets.UTF_8));
        } catch (IOException exception) {
            throw new UncheckedIOException(exception);
        }
    }
}
