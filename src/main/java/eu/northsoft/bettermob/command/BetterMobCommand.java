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
import eu.northsoft.bettermob.pack.PackValidator;
import eu.northsoft.bettermob.skill.SkillContext;
import eu.northsoft.bettermob.skill.SkillEngine;
import eu.northsoft.bettermob.skill.SkillRegistry;
import eu.northsoft.bettermob.skill.SkillStep;
import eu.northsoft.bettermob.spawner.Spawner;
import eu.northsoft.bettermob.spawner.SpawnerManager;
import eu.northsoft.bettermob.util.Tasks;
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
    private static final int MAX_SPAWN_AMOUNT = 100;

    static final Map<String, String> PERMISSIONS = permissions();

    private static Map<String, String> permissions() {
        Map<String, String> nodes = new LinkedHashMap<>();
        nodes.put("spawn", "bettermob.spawn");
        nodes.put("list", "bettermob.list");
        nodes.put("packs", "bettermob.list");
        nodes.put("info", "bettermob.list");
        nodes.put("reload", "bettermob.reload");
        nodes.put("validate", "bettermob.reload");
        nodes.put("skill", "bettermob.skill");
        nodes.put("give", "bettermob.give");
        nodes.put("killall", "bettermob.killall");
        nodes.put("spawner", "bettermob.spawner");
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
    private final SpawnerManager spawners;

    public BetterMobCommand(BetterMobPlugin plugin, MobManager manager, SkillRegistry skillRegistry, PackScanner packScanner, SkillEngine skillEngine, ItemRegistry itemRegistry, DropRegistry dropRegistry, SpawnerManager spawners) {
        this.plugin = plugin;
        this.messages = plugin.messages();
        this.manager = manager;
        this.skillRegistry = skillRegistry;
        this.packScanner = packScanner;
        this.skillEngine = skillEngine;
        this.itemRegistry = itemRegistry;
        this.dropRegistry = dropRegistry;
        this.spawners = spawners;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0) {
            boolean shown = false;
            shown |= help(sender, "spawn", "command.help.spawn");
            shown |= help(sender, "list", "command.help.list");
            shown |= help(sender, "packs", "command.help.packs");
            shown |= help(sender, "info", "command.help.info");
            shown |= help(sender, "reload", "command.help.reload");
            shown |= help(sender, "validate", "command.help.validate");
            shown |= help(sender, "skill", "command.help.skill");
            shown |= help(sender, "give", "command.help.give");
            shown |= help(sender, "killall", "command.help.killall");
            shown |= help(sender, "spawner", "command.help.spawner");
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
                int updated = plugin.reloadAll();
                messages.send(sender, "command.reloaded");
                if (updated > 0) messages.send(sender, "command.reloadedLiving", "count", updated);
            }
            case "list" -> messages.send(sender, "command.mobList", "mobs", String.join(", ", manager.registry().all().keySet()));
            case "packs" -> {
                List<String> packs = packScanner.listPacks();
                if (packs.isEmpty()) messages.send(sender, "command.packsNone");
                else packs.forEach(line -> messages.send(sender, "command.packEntry", "pack", line));
            }
            case "info" -> handleInfo(sender, args);
            case "validate" -> handleValidate(sender, args);
            case "spawn" -> handleSpawn(sender, args);
            case "skill" -> handleSkill(sender, args);
            case "give" -> handleGive(sender, args);
            case "killall" -> handleKillAll(sender, args);
            case "spawner" -> handleSpawner(sender, args);
            case "debug" -> handleDebug(sender, args);
            case "stats" -> handleStats(sender, args);
            default -> messages.send(sender, "command.unknown");
        }
        return true;
    }

    private void handleInfo(CommandSender sender, String[] args) {
        if (args.length < 2) {
            messages.send(sender, "command.info.usage");
            return;
        }
        MobDefinition mob = manager.registry().get(args[1]);
        if (mob == null) {
            messages.send(sender, "command.spawn.notRegistered", "mob", args[1]);
            return;
        }
        String none = messages.get("command.info.none");
        messages.send(sender, "command.info.header", "mob", mob.id, "alive", manager.aliveCounts().getOrDefault(mob.id, 0));
        messages.send(sender, "command.info.stats", "type", mob.type.name().toLowerCase(Locale.ROOT), "health", mob.health, "damage", mob.damage);
        messages.send(sender, "command.info.faction", "faction", mob.faction == null || mob.faction.isEmpty() ? none : mob.faction);
        messages.send(sender, "command.info.model", "model", mob.modelId == null || mob.modelId.isEmpty() ? none : mob.modelId);
        messages.send(sender, "command.info.equipment", "equipment", mob.equipment == null || mob.equipment.isEmpty() ? none : String.join(", ", mob.equipment));
        messages.send(sender, "command.info.drops", "count", mob.drops == null ? 0 : mob.drops.entries().size());
        for (MobDefinition.SkillTrigger.Trigger trigger : MobDefinition.SkillTrigger.Trigger.values()) {
            List<String> names = mob.triggersOf(trigger).stream().map(entry -> stepName(entry.step())).toList();
            if (!names.isEmpty()) messages.send(sender, "command.info.skills", "trigger", messages.get(triggerKey(trigger)), "skills", String.join(", ", names));
        }
    }

    private static String triggerKey(MobDefinition.SkillTrigger.Trigger trigger) {
        return switch (trigger) {
            case SPAWN -> "command.info.trigger.spawn";
            case LOAD -> "command.info.trigger.load";
            case INTERACT -> "command.info.trigger.interact";
            case DAMAGED -> "command.info.trigger.damaged";
            case ATTACK -> "command.info.trigger.attack";
            case DEATH -> "command.info.trigger.death";
            case TIMER -> "command.info.trigger.timer";
            case USE -> "command.info.trigger.use";
            case SHOOT -> "command.info.trigger.shoot";
        };
    }

    private String stepName(SkillStep step) {
        return step instanceof SkillStep.Mechanic mechanic ? mechanic.name() : messages.get("command.info.delay");
    }

    private static final int MAX_ISSUES_PER_PACK = 20;
    private static final int MAX_LINE_LENGTH = 70;

    private void handleValidate(CommandSender sender, String[] args) {
        Map<String, java.io.File> sources = packScanner.sources();
        Map<String, java.io.File> chosen = new LinkedHashMap<>();
        if (args.length >= 2) {
            for (Map.Entry<String, java.io.File> entry : sources.entrySet()) {
                if (entry.getKey().equalsIgnoreCase(args[1])) chosen.put(entry.getKey(), entry.getValue());
            }
            if (chosen.isEmpty()) {
                messages.send(sender, "command.validate.unknownPack", "pack", args[1]);
                return;
            }
        } else {
            chosen.putAll(sources);
        }

        PackValidator validator = new PackValidator(PackValidator.knowledgeOf(skillEngine, id -> skillRegistry.get(id) != null));
        List<PackValidator.Report> reports = new ArrayList<>();
        for (Map.Entry<String, java.io.File> entry : chosen.entrySet()) {
            String name = entry.getKey().isEmpty() ? messages.get("command.validate.mainName") : entry.getKey();
            PackValidator.Report report = validator.validatePack(name, entry.getValue());
            if (report.lines() > 0) reports.add(report);
        }

        int lines = reports.stream().mapToInt(PackValidator.Report::lines).sum();
        messages.send(sender, "command.validate.header", "lines", lines, "sources", reports.size());
        boolean anyProblem = false;
        for (PackValidator.Report report : reports) {
            messages.send(sender, "command.validate.pack", "pack", report.pack(), "lines", report.lines(), "problems", report.issues().size());
            int shown = 0;
            for (PackValidator.Issue issue : report.issues()) {
                anyProblem = true;
                if (shown++ >= MAX_ISSUES_PER_PACK) break;
                String line = issue.line().length() > MAX_LINE_LENGTH ? issue.line().substring(0, MAX_LINE_LENGTH) + "..." : issue.line();
                String reason = messages.get(reasonKey(issue.reason()), "name", issue.name());
                messages.send(sender, "command.validate.issue", "file", issue.file(), "line", line, "reason", reason);
            }
            if (report.issues().size() > MAX_ISSUES_PER_PACK) {
                messages.send(sender, "command.validate.more", "count", report.issues().size() - MAX_ISSUES_PER_PACK);
            }
        }
        if (!anyProblem) messages.send(sender, "command.validate.allGood");
    }

    private static String reasonKey(PackValidator.Reason reason) {
        return switch (reason) {
            case UNPARSEABLE -> "command.validate.reason.unparseable";
            case MECHANIC -> "command.validate.reason.mechanic";
            case TARGETER -> "command.validate.reason.targeter";
            case CONDITION -> "command.validate.reason.condition";
            case TARGETER_CONDITION -> "command.validate.reason.targeterCondition";
            case SKILL -> "command.validate.reason.skill";
        };
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
                amount = Math.max(1, Math.min(MAX_SPAWN_AMOUNT, Integer.parseInt(args[2])));
            } catch (NumberFormatException exception) {
                messages.send(sender, "command.spawn.usage");
                return;
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

    private void handleSpawner(CommandSender sender, String[] args) {
        String action = args.length < 2 ? "" : args[1].toLowerCase(Locale.ROOT);
        switch (action) {
            case "list" -> {
                if (spawners.all().isEmpty()) messages.send(sender, "command.spawner.none");
                for (Spawner spawner : spawners.all()) {
                    messages.send(sender, "command.spawner.entry", "id", spawner.id, "mob", spawner.mob, "world", spawner.world,
                            "x", (int) spawner.x, "y", (int) spawner.y, "z", (int) spawner.z,
                            "max", spawner.max, "interval", spawner.intervalSeconds);
                }
            }
            case "remove" -> {
                if (args.length < 3) messages.send(sender, "command.spawner.usage");
                else messages.send(sender, spawners.remove(args[2]) ? "command.spawner.removed" : "command.spawner.missing", "id", args[2]);
            }
            case "create" -> createSpawner(sender, args);
            default -> messages.send(sender, "command.spawner.usage");
        }
    }

    private void createSpawner(CommandSender sender, String[] args) {
        if (args.length < 4) {
            messages.send(sender, "command.spawner.usage");
            return;
        }
        if (!(sender instanceof Player player)) {
            messages.send(sender, "command.playersOnly");
            return;
        }
        MobDefinition definition = manager.registry().get(args[3]);
        if (definition == null) {
            messages.send(sender, "command.spawn.notRegistered", "mob", args[3]);
            return;
        }
        Spawner spawner;
        try {
            spawner = new Spawner(args[2], definition.id, player.getWorld().getName(),
                    player.getLocation().getBlockX() + 0.5, player.getLocation().getBlockY(), player.getLocation().getBlockZ() + 0.5,
                    optionalInt(args, 4, Spawner.DEFAULT_RADIUS), optionalInt(args, 5, Spawner.DEFAULT_INTERVAL_SECONDS),
                    optionalInt(args, 6, Spawner.DEFAULT_MAX), Spawner.DEFAULT_PLAYER_RANGE);
        } catch (NumberFormatException exception) {
            messages.send(sender, "command.spawner.usage");
            return;
        } catch (IllegalArgumentException exception) {
            messages.send(sender, "command.spawner.invalidId", "id", args[2]);
            return;
        }
        messages.send(sender, spawners.add(spawner) ? "command.spawner.created" : "command.spawner.exists", "id", spawner.id, "mob", spawner.mob);
    }

    private static int optionalInt(String[] args, int index, int fallback) {
        return args.length > index ? Integer.parseInt(args[index]) : fallback;
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
        int given = amount;
        Tasks.runOwned(plugin, receiver, () -> {
            receiver.getInventory().addItem(itemRegistry.create(definition, given)).values()
                    .forEach(rest -> receiver.getWorld().dropItemNaturally(receiver.getLocation(), rest));
            messages.send(sender, "command.give.done", "amount", given, "item", definition.id, "player", receiver.getName());
        }, () -> messages.send(sender, "command.playerOffline", "player", receiver.getName()));
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
        if (args.length == 2 && args[0].equalsIgnoreCase("validate")) return new ArrayList<>(packScanner.sources().keySet().stream().filter(name -> !name.isEmpty()).toList());
        if (args.length == 2 && args[0].equalsIgnoreCase("debug")) return List.of("off", "info", "verbose", "filter", "chat");
        if (args.length == 3 && args[0].equalsIgnoreCase("debug") && args[1].equalsIgnoreCase("filter")) {
            List<String> ids = new ArrayList<>(manager.registry().all().keySet());
            ids.addAll(skillRegistry.ids());
            for (Player player : Bukkit.getOnlinePlayers()) ids.add(player.getName());
            ids.add("clear");
            return ids;
        }
        if (args.length == 2 && args[0].equalsIgnoreCase("stats")) return List.of("on", "off", "reset");
        if (args.length == 2 && (args[0].equalsIgnoreCase("spawn") || args[0].equalsIgnoreCase("info"))) return new ArrayList<>(manager.registry().all().keySet());
        if (args.length == 2 && args[0].equalsIgnoreCase("skill")) return new ArrayList<>(skillRegistry.ids());
        if (args.length == 2 && args[0].equalsIgnoreCase("give")) return new ArrayList<>(itemRegistry.ids());
        if (args[0].equalsIgnoreCase("spawner")) {
            if (args.length == 2) return List.of("create", "remove", "list");
            if (args.length == 3 && args[1].equalsIgnoreCase("remove")) return spawners.all().stream().map(spawner -> spawner.id).toList();
            if (args.length == 4 && args[1].equalsIgnoreCase("create")) return new ArrayList<>(manager.registry().all().keySet());
            return List.of();
        }
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
