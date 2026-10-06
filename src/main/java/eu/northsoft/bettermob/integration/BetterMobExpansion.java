package eu.northsoft.bettermob.integration;

import eu.northsoft.bettermob.BetterMobPlugin;
import eu.northsoft.bettermob.item.ItemRegistry;
import eu.northsoft.bettermob.mob.MobManager;
import eu.northsoft.bettermob.skill.SkillRegistry;
import eu.northsoft.bettermob.stats.KillStats;
import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.OfflinePlayer;

import java.util.Locale;

public final class BetterMobExpansion extends PlaceholderExpansion {
    private final BetterMobPlugin plugin;
    private final MobManager manager;
    private final SkillRegistry skills;
    private final ItemRegistry items;
    private final KillStats kills;

    public BetterMobExpansion(BetterMobPlugin plugin, MobManager manager, SkillRegistry skills, ItemRegistry items, KillStats kills) {
        this.plugin = plugin;
        this.manager = manager;
        this.skills = skills;
        this.items = items;
        this.kills = kills;
    }

    @Override
    public String getIdentifier() {
        return "bettermob";
    }

    @Override
    public String getAuthor() {
        return "northsoft";
    }

    @Override
    public String getVersion() {
        return plugin.getPluginMeta().getVersion();
    }

    @Override
    public boolean persist() {
        return true;
    }

    @Override
    public String onRequest(OfflinePlayer player, String params) {
        String key = params.toLowerCase(Locale.ROOT);
        if (key.equals("mobs_alive")) return String.valueOf(manager.aliveCount());
        if (key.startsWith("mobs_alive_")) return String.valueOf(manager.aliveCount(key.substring("mobs_alive_".length())));
        if (key.equals("kills_total")) return player == null ? "0" : String.valueOf(kills.total(player.getUniqueId()));
        if (key.startsWith("kills_")) return player == null ? "0" : String.valueOf(kills.count(player.getUniqueId(), key.substring("kills_".length())));
        if (key.startsWith("top_")) return top(key.substring("top_".length()));
        return switch (key) {
            case "loaded_mobs" -> String.valueOf(manager.registry().all().size());
            case "loaded_skills" -> String.valueOf(skills.ids().size());
            case "loaded_items" -> String.valueOf(items.ids().size());
            default -> null;
        };
    }

    private String top(String spec) {
        int underscore = spec.indexOf('_');
        if (underscore < 0) return null;
        int place;
        try {
            place = Integer.parseInt(spec.substring(0, underscore));
        } catch (NumberFormatException exception) {
            return null;
        }
        String field = spec.substring(underscore + 1);
        if (place < 1 || (!field.equals("name") && !field.equals("kills"))) return null;
        var rows = kills.top(null, place);
        if (rows.size() < place) return field.equals("name") ? "-" : "0";
        var row = rows.get(place - 1);
        return field.equals("name") ? row.name() : String.valueOf(row.kills());
    }
}
