package eu.northsoft.bettermob.fabric;

import com.mojang.brigadier.arguments.StringArgumentType;
import eu.northsoft.bettermob.mob.MobDefinition;
import eu.northsoft.bettermob.platform.Position;
import eu.northsoft.bettermob.skill.core.CoreSkillEngine;
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
    private final FabricMobRegistry mobRegistry = new FabricMobRegistry();
    private final CoreSkillEngine skillEngine = new CoreSkillEngine();

    @Override
    public void onInitialize() {
        instance = this;
        this.platform = new FabricPlatform();
        this.mobRegistry.load();
        registerMechanics();
        registerCommands();
        LOGGER.info("BetterMob (Fabric) initialized for Minecraft {}", platform.minecraftVersion());
    }

    private void registerMechanics() {
        skillEngine.setWarningHandler(LOGGER::warn);

        skillEngine.registerMechanic("model", (ctx, target, params) -> {
            String modelId = params.getOrDefault("mid", params.get("m"));
            if (modelId != null && target.entity() != null && platform.model("bettermodel").isPresent()) {
                platform.model("bettermodel").get().attach(target.entity(), modelId);
            }
        });

        skillEngine.registerMechanic("damage", (ctx, target, params) -> {
            if (target.entity() instanceof eu.northsoft.bettermob.platform.PlatformLivingEntity living) {
                double amount = eu.northsoft.bettermob.skill.Params.parseNumber(params.getOrDefault("amount", params.get("a")), 1.0);
                living.damage(amount, ctx.caster());
            }
        });

        skillEngine.registerMechanic("heal", (ctx, target, params) -> {
            if (target.entity() instanceof eu.northsoft.bettermob.platform.PlatformLivingEntity living) {
                double amount = eu.northsoft.bettermob.skill.Params.parseNumber(params.getOrDefault("amount", params.get("a")), 1.0);
                living.health(Math.min(living.maxHealth(), living.health() + amount));
            }
        });
    }

    private void registerCommands() {
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> {
            dispatcher.register(CommandManager.literal("bettermob")
                .requires(CommandManager.requirePermissionLevel(CommandManager.GAMEMASTERS_CHECK))
                .then(CommandManager.literal("reload")
                    .executes(context -> {
                        mobRegistry.load();
                        int count = mobRegistry.all().size();
                        context.getSource().sendFeedback(() -> Text.literal("BetterMob configurations reloaded (" + count + " mobs)."), true);
                        return 1;
                    }))
                .then(CommandManager.literal("spawn")
                    .then(CommandManager.argument("mob", StringArgumentType.word())
                        .suggests((context, builder) -> net.minecraft.command.CommandSource.suggestMatching(mobRegistry.all().keySet(), builder))
                        .executes(context -> {
                            ServerCommandSource source = context.getSource();
                            String mobId = StringArgumentType.getString(context, "mob");
                            MobDefinition def = mobRegistry.get(mobId);
                            if (def == null) {
                                source.sendError(Text.literal("Unknown BetterMob: " + mobId));
                                return 0;
                            }
                            Vec3d pos = source.getPosition();
                            Position p = new Position(source.getWorld().getRegistryKey().getValue().toString(),
                                    pos.x, pos.y, pos.z, source.getEntity() != null ? source.getEntity().getYaw() : 0, 0);
                            var spawned = new FabricWorld(source.getWorld()).spawn(def.type, p);
                            if (spawned.isPresent()) {
                                var entity = spawned.get();
                                if (def.displayName != null && !def.displayName.isBlank()) {
                                    entity.customName(def.displayName.replace('&', '§'));
                                    entity.customNameVisible(true);
                                }
                                if (entity instanceof eu.northsoft.bettermob.platform.PlatformLivingEntity living) {
                                    if (def.health > 0) {
                                        living.maxHealth(def.health);
                                        living.health(def.health);
                                    }

                                    var ctx = new eu.northsoft.bettermob.skill.core.CoreSkillContext(living);
                                    for (var trigger : def.triggersOf(eu.northsoft.bettermob.mob.MobDefinition.SkillTrigger.Trigger.SPAWN)) {
                                        skillEngine.executeStep(trigger.step(), ctx);
                                    }
                                }
                                if (platform.model("bettermodel").isPresent() && def.modelId != null) {
                                    platform.model("bettermodel").get().attach(entity, def.modelId);
                                }
                                source.sendFeedback(() -> Text.literal("Spawned BetterMob: " + def.id + " (model: " + def.modelId + ")"), true);
                                return 1;
                            }
                            source.sendError(Text.literal("Failed to spawn BetterMob: " + mobId + " (unknown entity type: " + def.type + ")"));
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

    public FabricMobRegistry mobRegistry() {
        return mobRegistry;
    }

    public Map<String, MobDefinition> registry() {
        return mobRegistry.all();
    }
}
