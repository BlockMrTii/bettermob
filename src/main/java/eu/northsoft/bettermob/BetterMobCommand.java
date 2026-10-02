package eu.northsoft.bettermob;

import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

final class BetterMobCommand implements CommandExecutor, TabCompleter {
    private final BetterMobPlugin plugin;
    private final MobManager manager;
    private final SkillRegistry skillRegistry;
    private final PackScanner packScanner;
    private final SkillEngine skillEngine;
    private final ItemRegistry itemRegistry;
    private final DropRegistry dropRegistry;

    BetterMobCommand(BetterMobPlugin plugin, MobManager manager, SkillRegistry skillRegistry, PackScanner packScanner, SkillEngine skillEngine, ItemRegistry itemRegistry, DropRegistry dropRegistry) {
        this.plugin = plugin;
        this.manager = manager;
        this.skillRegistry = skillRegistry;
        this.packScanner = packScanner;
        this.skillEngine = skillEngine;
        this.itemRegistry = itemRegistry;
        this.dropRegistry = dropRegistry;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0) {
            sender.sendMessage("§7/bettermob spawn <id> [amount] §8- §7Mob spawnen");
            sender.sendMessage("§7/bettermob list §8- §7Verfuegbare Mobs anzeigen");
            sender.sendMessage("§7/bettermob packs §8- §7Geladene Packs anzeigen");
            sender.sendMessage("§7/bettermob reload §8- §7Mobs, Skills und Packs neu laden");
            sender.sendMessage("§7/bettermob skill <id> [Spieler] §8- §7Skill manuell ausloesen");
            sender.sendMessage("§7/bettermob give <item> [Spieler] [amount] §8- §7Item geben");
            return true;
        }

        switch (args[0].toLowerCase(Locale.ROOT)) {
            case "reload" -> {
                plugin.reloadConfig();
                skillRegistry.load();
                itemRegistry.load();
                dropRegistry.load();
                manager.registry().load();
                sender.sendMessage("§aBetterMob neu geladen.");
            }
            case "list" -> sender.sendMessage("§7Mobs: §f" + String.join(", ", manager.registry().all().keySet()));
            case "packs" -> {
                List<String> packs = packScanner.listPacks();
                if (packs.isEmpty()) sender.sendMessage("§7Keine Packs in packs/ gefunden.");
                else packs.forEach(line -> sender.sendMessage("§7- §f" + line));
            }
            case "spawn" -> handleSpawn(sender, args);
            case "skill" -> handleSkill(sender, args);
            case "give" -> handleGive(sender, args);
            default -> sender.sendMessage("§cUnbekannter Befehl.");
        }
        return true;
    }

    private void handleSpawn(CommandSender sender, String[] args) {
        if (args.length < 2) {
            sender.sendMessage("§cNutzung: /bettermob spawn <id> [amount]");
            return;
        }
        if (!(sender instanceof Player player)) {
            sender.sendMessage("§cNur von Spielern nutzbar.");
            return;
        }
        MobDefinition definition = manager.registry().get(args[1]);
        if (definition == null) {
            sender.sendMessage("§cMob '" + args[1] + "' ist nicht registriert.");
            return;
        }
        int amount = 1;
        if (args.length >= 3) {
            try {
                amount = Math.max(1, Integer.parseInt(args[2]));
            } catch (NumberFormatException ignored) {
            }
        }
        for (int i = 0; i < amount; i++) manager.spawn(definition, player.getLocation());
        sender.sendMessage("§a" + amount + "x '" + definition.id + "' gespawnt.");
    }

    private void handleSkill(CommandSender sender, String[] args) {
        if (args.length < 2) {
            sender.sendMessage("§cNutzung: /bettermob skill <id> [Spieler]");
            return;
        }
        String skillId = args[1];
        if (skillRegistry.get(skillId) == null) {
            sender.sendMessage("§cSkill '" + skillId + "' ist nicht registriert.");
            return;
        }

        Player caster;
        if (args.length >= 3) {
            caster = Bukkit.getPlayer(args[2]);
            if (caster == null) {
                sender.sendMessage("§cSpieler '" + args[2] + "' ist nicht online.");
                return;
            }
        } else if (sender instanceof Player player) {
            caster = player;
        } else {
            sender.sendMessage("§cOhne Spielerangabe nur von Spielern nutzbar.");
            return;
        }

        skillEngine.runById(skillId, SkillContext.of(caster));
        sender.sendMessage("§aSkill '" + skillId + "' auf " + caster.getName() + " ausgefuehrt.");
    }

    private void handleGive(CommandSender sender, String[] args) {
        if (args.length < 2) {
            sender.sendMessage("§cNutzung: /bettermob give <item> [Spieler] [amount]");
            return;
        }
        ItemDefinition definition = itemRegistry.get(args[1]);
        if (definition == null) {
            sender.sendMessage("§cItem '" + args[1] + "' ist nicht registriert.");
            return;
        }
        Player receiver;
        if (args.length >= 3) {
            receiver = Bukkit.getPlayer(args[2]);
            if (receiver == null) {
                sender.sendMessage("§cSpieler '" + args[2] + "' ist nicht online.");
                return;
            }
        } else if (sender instanceof Player player) {
            receiver = player;
        } else {
            sender.sendMessage("§cOhne Spielerangabe nur von Spielern nutzbar.");
            return;
        }
        int amount = 1;
        if (args.length >= 4) {
            try {
                amount = Math.max(1, Integer.parseInt(args[3]));
            } catch (NumberFormatException ignored) {
            }
        }
        receiver.getInventory().addItem(itemRegistry.create(definition, amount)).values()
                .forEach(rest -> receiver.getWorld().dropItemNaturally(receiver.getLocation(), rest));
        sender.sendMessage("§a" + amount + "x '" + definition.id + "' an " + receiver.getName() + " gegeben.");
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) return List.of("spawn", "list", "packs", "reload", "skill", "give");
        if (args.length == 2 && args[0].equalsIgnoreCase("spawn")) return new ArrayList<>(manager.registry().all().keySet());
        if (args.length == 2 && args[0].equalsIgnoreCase("skill")) return new ArrayList<>(skillRegistry.ids());
        if (args.length == 2 && args[0].equalsIgnoreCase("give")) return new ArrayList<>(itemRegistry.ids());
        if (args.length == 3 && (args[0].equalsIgnoreCase("skill") || args[0].equalsIgnoreCase("give"))) {
            List<String> names = new ArrayList<>();
            for (Player player : Bukkit.getOnlinePlayers()) names.add(player.getName());
            return names;
        }
        return List.of();
    }
}
