package eu.northsoft.bettermob;

import eu.northsoft.bettermob.api.BetterMobAPI;
import eu.northsoft.bettermob.command.BetterMobCommand;
import eu.northsoft.bettermob.debug.DebugManager;
import eu.northsoft.bettermob.drop.DropRegistry;
import eu.northsoft.bettermob.integration.BetterMobExpansion;
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
import eu.northsoft.bettermob.util.Tasks;
import org.bukkit.entity.ArmorStand;
import org.bukkit.command.PluginCommand;
import org.bukkit.plugin.ServicePriority;
import org.bukkit.plugin.java.JavaPlugin;

public final class BetterMobPlugin extends JavaPlugin {
    private DebugManager debug;
    private Messages messages;
    private BetterMobExpansion expansion;

    public DebugManager debug() {
        return debug;
    }

    public Messages messages() {
        return messages;
    }

    @Override
    public void onEnable() {
        saveDefaultConfig();
        messages = new Messages(this);
        debug = new DebugManager(this);
        PackScanner packScanner = new PackScanner(this);

        MobRegistry registry = new MobRegistry(this, packScanner);
        registry.load();

        BetterModelHook betterModel = new BetterModelHook(this);
        if (!betterModel.available()) messages.warn("plugin.betterModelMissing");
        ModelEngineHook modelEngine = new ModelEngineHook(this);

        SkillRegistry skillRegistry = new SkillRegistry(this, packScanner);
        skillRegistry.load();

        ItemRegistry itemRegistry = new ItemRegistry(this, packScanner);
        itemRegistry.load();

        DropRegistry dropRegistry = new DropRegistry(this, packScanner, itemRegistry);
        dropRegistry.load();

        MobManager manager = new MobManager(this, registry, betterModel, modelEngine, itemRegistry);
        SkillEngine skillEngine = new SkillEngine(this, skillRegistry, manager, betterModel, modelEngine, itemRegistry);
        manager.setSkillEngine(skillEngine);
        getServer().getPluginManager().registerEvents(new MobListener(manager, dropRegistry), this);
        getServer().getPluginManager().registerEvents(new ItemListener(itemRegistry, skillEngine), this);
        if (!Tasks.FOLIA) {
            Tasks.runGlobal(this, () -> getServer().getWorlds()
                    .forEach(world -> manager.removeLeftoverHelpers(world.getEntitiesByClass(ArmorStand.class))));
        }

        BetterMobCommand commandHandler = new BetterMobCommand(this, manager, skillRegistry, packScanner, skillEngine, itemRegistry, dropRegistry);
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
            expansion = new BetterMobExpansion(this, manager, skillRegistry, itemRegistry);
            expansion.register();
        }
    }

    @Override
    public void onDisable() {
        if (expansion != null) expansion.unregister();
    }
}
