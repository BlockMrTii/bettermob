package eu.northsoft.bettermob.command;

import eu.northsoft.bettermob.BetterMobPlugin;
import eu.northsoft.bettermob.debug.DebugManager;
import eu.northsoft.bettermob.drop.DropRegistry;
import eu.northsoft.bettermob.item.ItemDefinition;
import eu.northsoft.bettermob.item.ItemRegistry;
import eu.northsoft.bettermob.lang.Messages;
import eu.northsoft.bettermob.mob.MobDefinition;
import eu.northsoft.bettermob.mob.MobManager;
import eu.northsoft.bettermob.pack.PackScanner;
import eu.northsoft.bettermob.skill.SkillContext;
import eu.northsoft.bettermob.skill.SkillEngine;
import eu.northsoft.bettermob.skill.SkillRegistry;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public final class BetterMobCommand implements CommandExecutor, TabCompleter {
    private final BetterMobPlugin plugin;
    private final Messages messages;
    private final MobManager manager;
    private final SkillRegistry skillRegistry;
    private final PackScanner packScanner;
    private final SkillEngine skillEngine;
    private final ItemRegistry itemRegistry;
    private final DropRegistry dropRegistry;

    public BetterMobCommand(BetterMobPlugin plugin, MobManager manager, SkillRegistry skillRegistry, PackScanner packScanner, SkillEngine skillEngine, ItemRegistry itemRegistry, DropRegistry dropRegistry) {
        this.plugin = plugin;
        this.messages = plugin.messages();
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
            messages.send(sender, "command.help.spawn");
            messages.send(sender, "command.help.list");
            messages.send(sender, "command.help.packs");
            messages.send(sender, "command.help.reload");
            messages.send(sender, "command.help.skill");
            messages.send(sender, "command.help.give");
            messages.send(sender, "command.help.debug");
            return true;
        }

        switch (args[0].toLowerCase(Locale.ROOT)) {
            case "reload" -> {
                plugin.reloadConfig();
                messages.reload();
                plugin.debug().reload();
                skillRegistry.load();
                itemRegistry.load();
                dropRegistry.load();
                manager.registry().load();
                int updated = manager.reloadLiving();
                messages.send(sender, "command.reloaded");
                if (updated > 0) messages.send(sender, "command.reloadedLiving", "count", updated);
            }
            case "list" -> messages.send(sender, "command.mobList", "mobs", String.join(", ", manager.registry().all().keySet()));
            case "packs" -> {
                List<String> packs = packScanner.listPacks();
                if (packs.isEmpty()) messages.send(sender, "command.packsNone");
                else packs.forEach(line -> messages.send(sender, "command.packEntry", "pack", line));
            }
            case "spawn" -> handleSpawn(sender, args);
            case "skill" -> handleSkill(sender, args);
            case "give" -> handleGive(sender, args);
            case "debug" -> handleDebug(sender, args);
            default -> messages.send(sender, "command.unknown");
        }
        return true;
    }

    private void handleDebug(CommandSender sender, String[] args) {
        if (!sender.hasPermission("bettermob.debug")) {
            messages.send(sender, "command.noPermission");
            return;
        }
        DebugManager debug = plugin.debug();
        if (args.length < 2) {
            sendDebugStatus(sender, debug);
            return;
        }
        switch (args[1].toLowerCase(Locale.ROOT)) {
            case "off" -> debug.level(DebugManager.Level.OFF);
            case "on", "info" -> debug.level(DebugManager.Level.INFO);
            case "verbose" -> debug.level(DebugManager.Level.VERBOSE);
            case "filter" -> {
                if (args.length < 3 || args[2].equalsIgnoreCase("clear")) debug.filters().clear();
                else debug.filters().add(args[2].toLowerCase(Locale.ROOT));
            }
            case "chat" -> {
                if (!(sender instanceof Player player)) {
                    messages.send(sender, "command.playersOnly");
                    return;
                }
                messages.send(sender, debug.toggleWatcher(player) ? "command.debug.chatOn" : "command.debug.chatOff");
                return;
            }
            default -> {
                messages.send(sender, "command.debug.usage");
                return;
            }
        }
        sendDebugStatus(sender, debug);
    }

    private void sendDebugStatus(CommandSender sender, DebugManager debug) {
        String filters = debug.filters().isEmpty() ? messages.get("command.debug.filtersAll") : String.join(", ", debug.filters());
        messages.send(sender, "command.debug.status", "level", debug.level().name().toLowerCase(Locale.ROOT), "filters", filters);
    }

    private void handleSpawn(CommandSender sender, String[] args) {
        if (args.length < 2) {
            messages.send(sender, "command.spawn.usage");
            return;
        }
        if (!(sender instanceof Player player)) {
            messages.send(sender, "command.playersOnly");
            return;
        }
        MobDefinition definition = manager.registry().get(args[1]);
        if (definition == null) {
            messages.send(sender, "command.spawn.notRegistered", "mob", args[1]);
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
        messages.send(sender, "command.spawn.done", "amount", amount, "mob", definition.id);
    }

    private void handleSkill(CommandSender sender, String[] args) {
        if (args.length < 2) {
            messages.send(sender, "command.skill.usage");
            return;
        }
        String skillId = args[1];
        if (skillRegistry.get(skillId) == null) {
            messages.send(sender, "command.skill.notRegistered", "skill", skillId);
            return;
        }

        Player caster;
        if (args.length >= 3) {
            caster = Bukkit.getPlayer(args[2]);
            if (caster == null) {
                messages.send(sender, "command.playerOffline", "player", args[2]);
                return;
            }
        } else if (sender instanceof Player player) {
            caster = player;
        } else {
            messages.send(sender, "command.playerRequired");
            return;
        }

        skillEngine.runById(skillId, SkillContext.of(caster));
        messages.send(sender, "command.skill.done", "skill", skillId, "player", caster.getName());
    }

    private void handleGive(CommandSender sender, String[] args) {
        if (args.length < 2) {
            messages.send(sender, "command.give.usage");
            return;
        }
        ItemDefinition definition = itemRegistry.get(args[1]);
        if (definition == null) {
            messages.send(sender, "command.give.notRegistered", "item", args[1]);
            return;
        }
        Player receiver;
        if (args.length >= 3) {
            receiver = Bukkit.getPlayer(args[2]);
            if (receiver == null) {
                messages.send(sender, "command.playerOffline", "player", args[2]);
                return;
            }
        } else if (sender instanceof Player player) {
            receiver = player;
        } else {
            messages.send(sender, "command.playerRequired");
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
        messages.send(sender, "command.give.done", "amount", amount, "item", definition.id, "player", receiver.getName());
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) return List.of("spawn", "list", "packs", "reload", "skill", "give", "debug");
        if (args.length == 2 && args[0].equalsIgnoreCase("debug")) return List.of("off", "info", "verbose", "filter", "chat");
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
