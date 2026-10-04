package eu.northsoft.bettermob.mob;

import eu.northsoft.bettermob.BetterMobPlugin;
import eu.northsoft.bettermob.integration.PlaceholderHook;
import eu.northsoft.bettermob.util.Tasks;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.attribute.Attribute;
import org.bukkit.boss.BarFlag;
import org.bukkit.boss.BossBar;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

final class BossBarManager {
    private static final long UPDATE_TICKS = 10L;

    private static final class Shown {
        double progress = -1;
        String title;
    }

    private record Active(BossBar bar, Runnable cancel, Shown shown) {}

    private final BetterMobPlugin plugin;
    private final Map<UUID, Active> active = new ConcurrentHashMap<>();

    BossBarManager(BetterMobPlugin plugin) {
        this.plugin = plugin;
    }

    static String renderTitle(String template, String id, String name, double health, double maxHealth) {
        return template
                .replace("<mob.name>", name)
                .replace("<mob.id>", id)
                .replace("<mob.hp>", String.valueOf((long) Math.ceil(health)))
                .replace("<mob.maxhp>", String.valueOf((long) Math.ceil(maxHealth)))
                .replace('&', '§');
    }

    void attach(LivingEntity entity, MobDefinition definition) {
        detach(entity.getUniqueId());
        BossBarSettings settings = definition.bossBar;
        if (settings == null) return;
        BossBar bar = Bukkit.createBossBar(title(definition, entity.getHealth(), maxHealth(entity)), settings.color(), settings.style(), flags(settings));
        Shown shown = new Shown();
        Runnable cancel = Tasks.runTimer(plugin, entity, 1L, UPDATE_TICKS, () -> update(entity, definition, bar, shown));
        active.put(entity.getUniqueId(), new Active(bar, cancel, shown));
    }

    void detach(UUID entityId) {
        Active removed = active.remove(entityId);
        if (removed == null) return;
        removed.cancel().run();
        synchronized (removed.bar()) {
            removed.bar().removeAll();
        }
    }

    void clear() {
        for (UUID id : List.copyOf(active.keySet())) detach(id);
    }

    private void update(LivingEntity entity, MobDefinition definition, BossBar bar, Shown shown) {
        if (!entity.isValid()) {
            detach(entity.getUniqueId());
            return;
        }
        double health = entity.getHealth();
        double max = maxHealth(entity);
        double progress = max <= 0 ? 0 : Math.max(0, Math.min(1, health / max));
        String title = title(definition, health, max);
        synchronized (bar) {
            if (progress != shown.progress) {
                bar.setProgress(progress);
                shown.progress = progress;
            }
            if (!title.equals(shown.title)) {
                bar.setTitle(title);
                shown.title = title;
            }
            for (Player viewer : bar.getPlayers()) {
                if (!viewer.isOnline()) bar.removePlayer(viewer);
            }
        }

        double range = definition.bossBar.range();
        Location location = entity.getLocation();
        for (Player player : Bukkit.getOnlinePlayers()) {
            Tasks.runOwned(plugin, player, () -> updateViewer(entity.getUniqueId(), bar, player, location, range));
        }
    }

    private void updateViewer(UUID entityId, BossBar bar, Player player, Location barLocation, double range) {
        boolean inRange = player.getWorld().equals(barLocation.getWorld())
                && player.getLocation().distanceSquared(barLocation) <= range * range;
        synchronized (bar) {
            Active current = active.get(entityId);
            if (current == null || current.bar() != bar) return;
            if (inRange) bar.addPlayer(player);
            else bar.removePlayer(player);
        }
    }

    private String title(MobDefinition definition, double health, double max) {
        return PlaceholderHook.apply(null, renderTitle(definition.bossBar.title(), definition.id, definition.displayName, health, max));
    }

    private static double maxHealth(LivingEntity entity) {
        var attribute = entity.getAttribute(Attribute.MAX_HEALTH);
        return attribute == null ? entity.getHealth() : attribute.getValue();
    }

    private static BarFlag[] flags(BossBarSettings settings) {
        List<BarFlag> flags = new ArrayList<>();
        if (settings.createFog()) flags.add(BarFlag.CREATE_FOG);
        if (settings.darkenSky()) flags.add(BarFlag.DARKEN_SKY);
        if (settings.playMusic()) flags.add(BarFlag.PLAY_BOSS_MUSIC);
        return flags.toArray(new BarFlag[0]);
    }
}
