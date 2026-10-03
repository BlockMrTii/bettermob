package eu.northsoft.bettermob.integration;

import eu.northsoft.bettermob.BetterMobPlugin;
import eu.northsoft.bettermob.item.ItemRegistry;
import eu.northsoft.bettermob.mob.MobManager;
import eu.northsoft.bettermob.skill.SkillRegistry;
import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.OfflinePlayer;

import java.util.Locale;

public final class BetterMobExpansion extends PlaceholderExpansion {
    private final BetterMobPlugin plugin;
    private final MobManager manager;
    private final SkillRegistry skills;
    private final ItemRegistry items;

    public BetterMobExpansion(BetterMobPlugin plugin, MobManager manager, SkillRegistry skills, ItemRegistry items) {
        this.plugin = plugin;
        this.manager = manager;
        this.skills = skills;
        this.items = items;
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
        return switch (key) {
            case "loaded_mobs" -> String.valueOf(manager.registry().all().size());
            case "loaded_skills" -> String.valueOf(skills.ids().size());
            case "loaded_items" -> String.valueOf(items.ids().size());
            default -> null;
        };
    }
}
