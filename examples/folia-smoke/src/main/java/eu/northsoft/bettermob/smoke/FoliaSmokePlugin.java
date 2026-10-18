package eu.northsoft.bettermob.smoke;

import eu.northsoft.bettermob.api.BetterMobAPI;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityRemoveEvent;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.Optional;
import java.util.concurrent.TimeUnit;

public final class FoliaSmokePlugin extends JavaPlugin implements Listener {
    private static final String MOB = "smoke_mob";
    private static final String SKILL = "smoke_skill";

    @Override
    public void onEnable() {
        getServer().getPluginManager().registerEvents(this, this);
        Bukkit.getGlobalRegionScheduler().runDelayed(this, task -> start(), 100L);
    }

    @EventHandler
    public void onRemove(EntityRemoveEvent event) {
        if (event.getEntity().getType() == EntityType.ZOMBIE) getLogger().info("SMOKE-REMOVED cause=" + event.getCause());
    }

    private void start() {
        Location at = Bukkit.getWorlds().get(0).getSpawnLocation();
        at.getWorld().getChunkAtAsync(at).thenAccept(chunk -> {
            at.getWorld().addPluginChunkTicket(chunk.getX(), chunk.getZ(), this);
            Bukkit.getRegionScheduler().run(this, at, task -> spawn(at));
        });
    }

    private void spawn(Location at) {
        Optional<LivingEntity> spawned = BetterMobAPI.get().spawn(MOB, at);
        if (spawned.isEmpty()) {
            getLogger().severe("SMOKE-FAIL could not spawn " + MOB);
            finish();
            return;
        }
        LivingEntity mob = spawned.get();
        getLogger().info("SMOKE-SPAWNED " + MOB);
        Runnable retired = () -> getLogger().info("SMOKE-RETIRED the mob was removed before a task ran");
        mob.getScheduler().run(this, task -> getLogger().info("SMOKE-STATE valid=" + mob.isValid() + " health=" + mob.getHealth()), retired);
        mob.getScheduler().runDelayed(this, task -> getLogger().info("SMOKE-SKILL " + BetterMobAPI.get().runSkill(SKILL, mob)), retired, 20L);
        mob.getScheduler().runDelayed(this, task -> mob.damage(4.0), retired, 40L);
        mob.getScheduler().runDelayed(this, task -> mob.damage(4.0), retired, 60L);
        mob.getScheduler().runDelayed(this, task -> mob.remove(), retired, 100L);
        Bukkit.getGlobalRegionScheduler().runDelayed(this, task -> finish(), 160L);
    }

    private void finish() {
        getLogger().info("SMOKE-DONE");
        Bukkit.getAsyncScheduler().runDelayed(this, task -> Bukkit.getGlobalRegionScheduler().run(this, stop -> Bukkit.shutdown()), 1, TimeUnit.SECONDS);
    }
}
