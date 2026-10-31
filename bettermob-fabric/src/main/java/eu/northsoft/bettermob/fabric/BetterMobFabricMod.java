package eu.northsoft.bettermob.fabric;

import com.mojang.brigadier.arguments.StringArgumentType;
import eu.northsoft.bettermob.mob.MobDefinition;
import eu.northsoft.bettermob.platform.Position;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.text.Text;
import net.minecraft.util.math.Vec3d;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public final class BetterMobFabricMod implements ModInitializer {
    public static final String MOD_ID = "bettermob";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    private static BetterMobFabricMod instance;
    private FabricPlatform platform;
    private final Map<String, MobDefinition> registry = new ConcurrentHashMap<>();

    @Override
    public void onInitialize() {
        instance = this;
        this.platform = new FabricPlatform();
        registerCommands();
        LOGGER.info("BetterMob (Fabric) initialized for Minecraft {}", platform.minecraftVersion());
    }

    private void registerCommands() {
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> {
            dispatcher.register(CommandManager.literal("bettermob")
                .requires(CommandManager.requirePermissionLevel(CommandManager.GAMEMASTERS_CHECK))
                .then(CommandManager.literal("spawn")
                    .then(CommandManager.argument("mob", StringArgumentType.word())
                        .executes(context -> {
                            ServerCommandSource source = context.getSource();
                            String mobId = StringArgumentType.getString(context, "mob");
                            MobDefinition def = registry.get(mobId.toLowerCase());
                            if (def == null) {
                                def = new MobDefinition(mobId, "minecraft:zombie", mobId, mobId, 20.0, 2.0, false,
                                        List.of(), List.of(), MobDefinition.Options.DEFAULT,
                                        false, Map.of(), List.of(), null, null, null,
                                        List.of(), null, null, null);
                            }
                            Vec3d pos = source.getPosition();
                            Position p = new Position(source.getWorld().getRegistryKey().getValue().toString(),
                                    pos.x, pos.y, pos.z, source.getEntity() != null ? source.getEntity().getYaw() : 0, 0);
                            var spawned = new FabricWorld(source.getWorld()).spawn(def.type, p);
                            if (spawned.isPresent()) {
                                if (platform.model("bettermodel").isPresent() && def.modelId != null) {
                                    platform.model("bettermodel").get().attach(spawned.get(), def.modelId);
                                }
                                source.sendFeedback(() -> Text.literal("Spawned BetterMob: " + mobId), true);
                                return 1;
                            }
                            source.sendError(Text.literal("Failed to spawn BetterMob: " + mobId));
                            return 0;
                        }))));
        });
    }

    public static BetterMobFabricMod instance() {
        return instance;
    }

    public FabricPlatform platform() {
        return platform;
    }

    public Map<String, MobDefinition> registry() {
        return registry;
    }
}
