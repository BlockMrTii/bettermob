package eu.northsoft.bettermob.debug;

import eu.northsoft.bettermob.BetterMobPlugin;
import eu.northsoft.bettermob.mob.MobManager;
import eu.northsoft.bettermob.util.RegionEntities;
import eu.northsoft.bettermob.util.Tasks;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Mob;
import org.bukkit.entity.Player;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class AggroView {
    static final int RANGE = 48;
    static final int MAX_MOBS = 30;
    static final int MAX_POINTS = 60;
    static final double STEP = 0.75;
    private static final long PERIOD_TICKS = 10L;

    private final BetterMobPlugin plugin;
    private final MobManager manager;
    private final Map<UUID, Runnable> active = new ConcurrentHashMap<>();

    public AggroView(BetterMobPlugin plugin, MobManager manager) {
        this.plugin = plugin;
        this.manager = manager;
    }

    public boolean toggle(Player player) {
        Runnable running = active.remove(player.getUniqueId());
        if (running != null) {
            running.run();
            return false;
        }
        UUID id = player.getUniqueId();
        Runnable[] cancel = {() -> { }};
        cancel[0] = Tasks.runTimer(plugin, player, PERIOD_TICKS, PERIOD_TICKS, () -> {
            if (!player.isOnline()) {
                active.remove(id);
                cancel[0].run();
                return;
            }
            draw(player);
        });
        active.put(id, cancel[0]);
        return true;
    }

    public void stopAll() {
        active.values().forEach(Runnable::run);
        active.clear();
    }

    private void draw(Player player) {
        int shown = 0;
        for (Entity entity : RegionEntities.near(player.getLocation(), RANGE)) {
            if (shown >= MAX_MOBS) break;
            if (!(entity instanceof Mob mob) || manager.definitionOf(mob.getUniqueId()) == null) continue;
            LivingEntity target = mob.getTarget();
            if (target == null || !target.getWorld().equals(mob.getWorld())) continue;
            shown++;
            line(player, mob.getEyeLocation(), target.getEyeLocation());
        }
    }

    static int points(double length) {
        return (int) Math.max(1, Math.min(MAX_POINTS, Math.ceil(length / STEP)));
    }

    private static void line(Player player, Location from, Location to) {
        double length = from.distance(to);
        int points = points(length);
        double dx = (to.getX() - from.getX()) / points;
        double dy = (to.getY() - from.getY()) / points;
        double dz = (to.getZ() - from.getZ()) / points;
        for (int i = 1; i <= points; i++) {
            player.spawnParticle(Particle.END_ROD, from.getX() + dx * i, from.getY() + dy * i, from.getZ() + dz * i, 1, 0, 0, 0, 0);
        }
    }
}
