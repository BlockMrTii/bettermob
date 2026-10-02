package eu.northsoft.bettermob;

import eu.northsoft.bettermob.api.BetterMobAPI;
import org.bukkit.command.PluginCommand;
import org.bukkit.plugin.ServicePriority;
import org.bukkit.plugin.java.JavaPlugin;

public final class BetterMobPlugin extends JavaPlugin {
    @Override
    public void onEnable() {
        saveDefaultConfig();
        PackScanner packScanner = new PackScanner(this);

        MobRegistry registry = new MobRegistry(this, packScanner);
        registry.load();

        BetterModelHook betterModel = new BetterModelHook(this);
        if (!betterModel.available()) getLogger().warning("BetterModel ist nicht installiert oder aktiv - Mobs spawnen ohne Modell.");
        ModelEngineHook modelEngine = new ModelEngineHook(this);

        SkillRegistry skillRegistry = new SkillRegistry(this, packScanner);
        skillRegistry.load();

        ItemRegistry itemRegistry = new ItemRegistry(this, packScanner);
        itemRegistry.load();

        MobManager manager = new MobManager(this, registry, betterModel, modelEngine, itemRegistry);
        SkillEngine skillEngine = new SkillEngine(this, skillRegistry, manager, betterModel, modelEngine, itemRegistry);
        manager.setSkillEngine(skillEngine);
        getServer().getPluginManager().registerEvents(new MobListener(manager), this);
        getServer().getPluginManager().registerEvents(new ItemListener(itemRegistry, skillEngine), this);

        BetterMobCommand commandHandler = new BetterMobCommand(this, manager, skillRegistry, packScanner, skillEngine, itemRegistry);
        PluginCommand command = getCommand("bettermob");
        if (command == null) {
            getLogger().severe("Command /bettermob fehlt in plugin.yml.");
            getServer().getPluginManager().disablePlugin(this);
            return;
        }
        command.setExecutor(commandHandler);
        command.setTabCompleter(commandHandler);

        BetterMobApiImpl api = new BetterMobApiImpl(this, manager, skillRegistry, skillEngine);
        getServer().getServicesManager().register(BetterMobAPI.class, api, this, ServicePriority.Normal);
        getServer().getPluginManager().registerEvents(api, this);
    }
}
