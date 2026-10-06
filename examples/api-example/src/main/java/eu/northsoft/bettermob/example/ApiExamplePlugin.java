package eu.northsoft.bettermob.example;

import eu.northsoft.bettermob.api.BetterMobAPI;
import eu.northsoft.bettermob.api.MobInfo;
import eu.northsoft.bettermob.api.event.BetterMobDeathEvent;
import eu.northsoft.bettermob.api.event.BetterMobSpawnEvent;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public final class ApiExamplePlugin extends JavaPlugin implements Listener {
    @Override
    public void onEnable() {
        BetterMobAPI api = BetterMobAPI.get();
        api.registerMechanic(this, "heal", context -> {
            double amount = Double.parseDouble(context.params().getOrDefault("amount", "2"));
            if (context.target() instanceof LivingEntity living) living.heal(amount);
        });
        api.registerCondition(this, "daytime", context -> context.caster().getWorld().isDayTime());
        api.registerTargeter(this, "nearestplayer", context -> {
            List<Entity> nearest = new ArrayList<>();
            context.caster().getWorld().getPlayers().stream()
                    .filter(player -> !player.isDead())
                    .min(Comparator.comparingDouble(player -> player.getLocation().distanceSquared(context.caster().getLocation())))
                    .ifPresent(nearest::add);
            return nearest;
        });
        api.registerPlaceholder(this, "example", (key, context) -> key.equals("world") ? context.caster().getWorld().getName() : null);
        getServer().getPluginManager().registerEvents(this, this);
        registerModernEvents();
    }

    private void registerModernEvents() {
        try {
            Listener listener = (Listener) Class.forName("eu.northsoft.bettermob.example.ModernEvents").getDeclaredConstructor().newInstance();
            getServer().getPluginManager().registerEvents(listener, this);
        } catch (ReflectiveOperationException | LinkageError exception) {
            getLogger().info("Damage and skill events are left out: this build was compiled against a BetterMob without them.");
        }
    }

    @EventHandler
    public void onSpawn(BetterMobSpawnEvent event) {
        MobInfo mob = event.getMob();
        getLogger().info(mob.id() + " spawned at " + event.getEntity().getLocation().toVector());
    }

    @EventHandler
    public void onDeath(BetterMobDeathEvent event) {
        Player killer = event.getEntity().getKiller();
        if (killer != null) killer.sendMessage("You killed " + event.getMob().id() + ".");
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player) || args.length != 1) {
            sender.sendMessage("Usage: /" + label + " <mob>, as a player");
            return true;
        }
        boolean spawned = BetterMobAPI.get().spawn(args[0], player.getLocation()).isPresent();
        sender.sendMessage(spawned ? "Spawned " + args[0] + "." : "No mob called " + args[0] + ".");
        return true;
    }
}
