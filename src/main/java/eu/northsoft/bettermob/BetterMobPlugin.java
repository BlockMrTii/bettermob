package eu.northsoft.bettermob;

import eu.northsoft.bettermob.api.BetterMobAPI;
import eu.northsoft.bettermob.command.BetterMobCommand;
import eu.northsoft.bettermob.debug.DebugManager;
import eu.northsoft.bettermob.drop.DropRegistry;
import eu.northsoft.bettermob.integration.BetterMobExpansion;
import eu.northsoft.bettermob.item.EggItems;
import eu.northsoft.bettermob.item.EggListener;
import eu.northsoft.bettermob.item.ItemListener;
import eu.northsoft.bettermob.item.ItemRegistry;
import eu.northsoft.bettermob.lang.Messages;
import eu.northsoft.bettermob.mob.MobListener;
import eu.northsoft.bettermob.mob.MobManager;
import eu.northsoft.bettermob.mob.MobRegistry;
import eu.northsoft.bettermob.model.BetterModelHook;
import eu.northsoft.bettermob.model.ModelEngineHook;
import eu.northsoft.bettermob.pack.PackScanner;
import eu.northsoft.bettermob.service.BetterMobApiImpl;
import eu.northsoft.bettermob.skill.SkillEngine;
import eu.northsoft.bettermob.skill.SkillRegistry;
import eu.northsoft.bettermob.spawner.SpawnerListener;
import eu.northsoft.bettermob.spawner.SpawnerManager;
import eu.northsoft.bettermob.stats.KillStats;
import eu.northsoft.bettermob.stats.SkillStats;
import eu.northsoft.bettermob.util.ReloadDiff;
import eu.northsoft.bettermob.util.Tasks;
import org.bstats.bukkit.Metrics;
import org.bukkit.entity.ArmorStand;
import org.bukkit.command.PluginCommand;
import org.bukkit.plugin.ServicePriority;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.util.Map;
import java.util.List;

public final class BetterMobPlugin extends JavaPlugin {
    private static final long KILLS_SAVE_TICKS = 6000L;

    private DebugManager debug;
    private SkillStats stats;
    private KillStats kills;
    private eu.northsoft.bettermob.debug.AggroView aggroView;
    private File killsFile;
    private Messages messages;
    private BetterMobExpansion expansion;
    private MobManager manager;
    private SkillRegistry skillRegistry;
    private ItemRegistry itemRegistry;
    private DropRegistry dropRegistry;
    private SpawnerManager spawners;

    public DebugManager debug() {
        return debug;
    }

    public KillStats kills() {
        return kills;
    }

    public void saveKills(boolean async) {
        if (kills == null || !kills.dirty()) return;
        Runnable write = () -> {
            try {
                kills.flush(killsFile);
            } catch (java.io.IOException exception) {
                messages.warn("kills.saveFailed", "error", exception);
            }
        };
        if (async) Tasks.runAsync(this, write);
        else write.run();
    }

    public SkillStats stats() {
        return stats;
    }

    public Messages messages() {
        return messages;
    }

    @Override
    public void onEnable() {
        int pluginId = 34490;
        new Metrics(this, pluginId);
        saveDefaultConfig();
        messages = new Messages(this);
        debug = new DebugManager(this);
        stats = new SkillStats();
        stats.reload(getConfig());
        kills = new KillStats();
        killsFile = new File(getDataFolder(), "kills.yml");
        if (killsFile.isFile()) {
            try {
                var loaded = new org.bukkit.configuration.file.YamlConfiguration();
                loaded.load(killsFile);
                kills.load(loaded);
            } catch (java.io.IOException | org.bukkit.configuration.InvalidConfigurationException exception) {
                kills.protect();
                messages.warn("kills.loadFailed", "error", exception);
            }
        }
        Tasks.runGlobalTimer(this, KILLS_SAVE_TICKS, () -> saveKills(true));
        PackScanner packScanner = new PackScanner(this);

        MobRegistry registry = new MobRegistry(this, packScanner);
        registry.load();

        BetterModelHook betterModel = new BetterModelHook(this);
        if (!betterModel.available()) messages.warn("plugin.betterModelMissing");
        ModelEngineHook modelEngine = new ModelEngineHook(this);

        skillRegistry = new SkillRegistry(this, packScanner);
        skillRegistry.load();

        itemRegistry = new ItemRegistry(this, packScanner);
        itemRegistry.load();

        dropRegistry = new DropRegistry(this, packScanner, itemRegistry);
        dropRegistry.load();

        manager = new MobManager(this, registry, betterModel, modelEngine, itemRegistry);
        SkillEngine skillEngine = new SkillEngine(this, skillRegistry, manager, betterModel, modelEngine, itemRegistry);
        skillEngine.setDrops(dropRegistry);
        manager.setSkillEngine(skillEngine);
        getServer().getPluginManager().registerEvents(new MobListener(manager, dropRegistry, kills), this);
        getServer().getPluginManager().registerEvents(new ItemListener(itemRegistry, skillEngine), this);
        EggItems eggItems = new EggItems(this);
        getServer().getPluginManager().registerEvents(new EggListener(this, manager, eggItems), this);
        if (!Tasks.FOLIA) {
            Tasks.runGlobal(this, () -> getServer().getWorlds()
                    .forEach(world -> manager.removeLeftoverHelpers(world.getEntitiesByClass(ArmorStand.class))));
        }

        spawners = new SpawnerManager(this, manager);
        spawners.load();
        getServer().getPluginManager().registerEvents(new SpawnerListener(spawners), this);
        spawners.start();

        aggroView = new eu.northsoft.bettermob.debug.AggroView(this, manager);
        getServer().getPluginManager().registerEvents(aggroView, this);
        BetterMobCommand commandHandler = new BetterMobCommand(this, manager, skillRegistry, packScanner, skillEngine, itemRegistry, dropRegistry, spawners, eggItems, aggroView);
        PluginCommand command = getCommand("bettermob");
        if (command == null) {
            messages.severe("plugin.commandMissing");
            getServer().getPluginManager().disablePlugin(this);
            return;
        }
        command.setExecutor(commandHandler);
        command.setTabCompleter(commandHandler);

        BetterMobApiImpl api = new BetterMobApiImpl(this, manager, skillRegistry, skillEngine);
        getServer().getServicesManager().register(BetterMobAPI.class, api, this, ServicePriority.Normal);
        getServer().getPluginManager().registerEvents(api, this);

        if (getServer().getPluginManager().isPluginEnabled("PlaceholderAPI")) {
            expansion = new BetterMobExpansion(this, manager, skillRegistry, itemRegistry, kills);
            expansion.register();
        }
    }

    public record ReloadReport(int updated, Map<String, ReloadDiff> diffs, int affectedLiving) {}

    public ReloadReport reloadAll() {
        reloadConfig();
        messages.reload();
        debug.reload();
        stats.reload(getConfig());
        var before = List.of(manager.registry().fingerprints(), skillRegistry.fingerprints(), itemRegistry.fingerprints(), dropRegistry.fingerprints());
        skillRegistry.load();
        itemRegistry.load();
        dropRegistry.load();
        manager.registry().load();
        spawners.load();
        Map<String, ReloadDiff> diffs = new java.util.LinkedHashMap<>();
        diffs.put("mobs", ReloadDiff.of(before.get(0), manager.registry().fingerprints()));
        diffs.put("skills", ReloadDiff.of(before.get(1), skillRegistry.fingerprints()));
        diffs.put("items", ReloadDiff.of(before.get(2), itemRegistry.fingerprints()));
        diffs.put("droptables", ReloadDiff.of(before.get(3), dropRegistry.fingerprints()));
        ReloadDiff mobs = diffs.get("mobs");
        int affected = manager.livingOf(new java.util.HashSet<>(mobs.changed()));
        return new ReloadReport(manager.reloadLiving(), diffs, affected);
    }

    @Override
    public void onDisable() {
        saveKills(false);
        if (aggroView != null) aggroView.stopAll();
        if (spawners != null) spawners.stop();
        if (expansion != null) expansion.unregister();
        if (manager != null) manager.shutdown();
    }
}