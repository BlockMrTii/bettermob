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
import eu.northsoft.bettermob.stats.SkillStats;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public final class BetterMobCommand implements CommandExecutor, TabCompleter {
    private static final int TIMING_ROWS = 10;

    static final Map<String, String> PERMISSIONS = permissions();

    private static Map<String, String> permissions() {
        Map<String, String> nodes = new LinkedHashMap<>();
        nodes.put("spawn", "bettermob.spawn");
        nodes.put("list", "bettermob.list");
        nodes.put("packs", "bettermob.list");
        nodes.put("reload", "bettermob.reload");
        nodes.put("skill", "bettermob.skill");
        nodes.put("give", "bettermob.give");
        nodes.put("killall", "bettermob.killall");
        nodes.put("debug", "bettermob.debug");
        nodes.put("stats", "bettermob.debug");
        return nodes;
    }

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
            boolean shown = false;
            shown |= help(sender, "spawn", "command.help.spawn");
            shown |= help(sender, "list", "command.help.list");
            shown |= help(sender, "packs", "command.help.packs");
            shown |= help(sender, "reload", "command.help.reload");
            shown |= help(sender, "skill", "command.help.skill");
            shown |= help(sender, "give", "command.help.give");
            shown |= help(sender, "killall", "command.help.killall");
            shown |= help(sender, "debug", "command.help.debug");
            shown |= help(sender, "stats", "command.help.stats");
            if (!shown) messages.send(sender, "command.noPermission");
            return true;
        }

        String subcommand = args[0].toLowerCase(Locale.ROOT);
        String node = PERMISSIONS.get(subcommand);
        if (node != null && !sender.hasPermission(node)) {
            messages.send(sender, "command.noPermission");
            return true;
        }

        switch (subcommand) {
            case "reload" -> {
                plugin.reloadConfig();
                messages.reload();
                plugin.debug().reload();
                plugin.stats().reload(plugin.getConfig());
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
            case "killall" -> handleKillAll(sender, args);
            case "debug" -> handleDebug(sender, args);
            case "stats" -> handleStats(sender, args);
            default -> messages.send(sender, "command.unknown");
        }
        return true;
    }

    private void handleStats(CommandSender sender, String[] args) {
        SkillStats stats = plugin.stats();
        if (args.length >= 2) {
            switch (args[1].toLowerCase(Locale.ROOT)) {
                case "on" -> {
                    stats.enabled(true);
                    messages.send(sender, "command.stats.enabled");
                }
                case "off" -> {
                    stats.enabled(false);
                    messages.send(sender, "command.stats.disabled");
                }
                case "reset" -> {
                    stats.reset();
                    messages.send(sender, "command.stats.reset");
                }
                default -> messages.send(sender, "command.stats.usage");
            }
            return;
        }
        messages.send(sender, "command.stats.header");
        Map<String, Integer> counts = manager.aliveCounts();
        messages.send(sender, "command.stats.mobs", "total", counts.values().stream().mapToInt(Integer::intValue).sum());
        counts.forEach((mob, count) -> messages.send(sender, "command.stats.mobEntry", "mob", mob, "count", count));
        messages.send(sender, "command.stats.overview", "timers", manager.timerCount(),
                "pending", stats.enabled() ? stats.pending() : "-",
                "skills", skillRegistry.ids().size(), "packs", packScanner.listPacks().size());
        if (!stats.enabled()) {
            messages.send(sender, "command.stats.profilingOff");
            return;
        }
        List<SkillStats.Row> rows = stats.top(TIMING_ROWS);
        if (rows.isEmpty()) {
            messages.send(sender, "command.stats.timingNone");
            return;
        }
        messages.send(sender, "command.stats.timingHeader", "limit", TIMING_ROWS);
        for (SkillStats.Row row : rows) {
            messages.send(sender, "command.stats.timingEntry", "skill", row.skill(), "calls", row.calls(),
                    "avg", millis(row.averageMillis()), "max", millis(row.maxMillis()), "total", millis(row.totalMillis()));
        }
    }

    private static String millis(double value) {
        return String.format(Locale.ROOT, "%.2f", value);
    }

    private boolean help(CommandSender sender, String subcommand, String key) {
        if (!sender.hasPermission(PERMISSIONS.get(subcommand))) return false;
        messages.send(sender, key);
        return true;
    }

    private void handleDebug(CommandSender sender, String[] args) {
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

    private void handleKillAll(CommandSender sender, String[] args) {
        String mobId = args.length >= 2 && !args[1].equals("*") ? args[1] : null;
        World world = null;
        if (args.length >= 3) {
            world = Bukkit.getWorld(args[2]);
            if (world == null) {
                messages.send(sender, "command.killall.worldMissing", "world", args[2]);
                return;
            }
        }
        if (mobId != null && manager.registry().get(mobId) == null) {
            messages.send(sender, "command.spawn.notRegistered", "mob", mobId);
            return;
        }
        messages.send(sender, "command.killall.done", "count", manager.killAll(mobId, world));
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
        if (args.length == 1) {
            return PERMISSIONS.entrySet().stream()
                    .filter(entry -> sender.hasPermission(entry.getValue()))
                    .map(Map.Entry::getKey)
                    .toList();
        }
        String node = PERMISSIONS.get(args[0].toLowerCase(Locale.ROOT));
        if (node == null || !sender.hasPermission(node)) return List.of();
        if (args.length == 2 && args[0].equalsIgnoreCase("debug")) return List.of("off", "info", "verbose", "filter", "chat");
        if (args.length == 2 && args[0].equalsIgnoreCase("stats")) return List.of("on", "off", "reset");
        if (args.length == 2 && args[0].equalsIgnoreCase("spawn")) return new ArrayList<>(manager.registry().all().keySet());
        if (args.length == 2 && args[0].equalsIgnoreCase("skill")) return new ArrayList<>(skillRegistry.ids());
        if (args.length == 2 && args[0].equalsIgnoreCase("give")) return new ArrayList<>(itemRegistry.ids());
        if (args.length == 2 && args[0].equalsIgnoreCase("killall")) {
            List<String> ids = new ArrayList<>(manager.registry().all().keySet());
            ids.add("*");
            return ids;
        }
        if (args.length == 3 && args[0].equalsIgnoreCase("killall")) {
            return Bukkit.getWorlds().stream().map(World::getName).toList();
        }
        if (args.length == 3 && (args[0].equalsIgnoreCase("skill") || args[0].equalsIgnoreCase("give"))) {
            List<String> names = new ArrayList<>();
            for (Player player : Bukkit.getOnlinePlayers()) names.add(player.getName());
            return names;
        }
        return List.of();
    }
}
