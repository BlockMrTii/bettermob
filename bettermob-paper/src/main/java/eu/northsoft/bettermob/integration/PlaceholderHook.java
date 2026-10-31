package eu.northsoft.bettermob.integration;

import me.clip.placeholderapi.PlaceholderAPI;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;

public final class PlaceholderHook {
    private PlaceholderHook() {}

    public static String apply(OfflinePlayer player, String text) {
        if (text == null || text.indexOf('%') < 0) return text;
        if (!Bukkit.getPluginManager().isPluginEnabled("PlaceholderAPI")) return text;
        return Papi.apply(player, text);
    }

    private static final class Papi {
        static String apply(OfflinePlayer player, String text) {
            return PlaceholderAPI.setPlaceholders(player, text);
        }
    }
}
