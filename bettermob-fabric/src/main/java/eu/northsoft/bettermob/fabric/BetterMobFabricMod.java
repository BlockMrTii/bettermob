package eu.northsoft.bettermob.fabric;

import net.fabricmc.api.ModInitializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class BetterMobFabricMod implements ModInitializer {
    public static final String MOD_ID = "bettermob";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    private static BetterMobFabricMod instance;
    private FabricPlatform platform;

    @Override
    public void onInitialize() {
        instance = this;
        this.platform = new FabricPlatform();
        LOGGER.info("BetterMob (Fabric) initialized for Minecraft {}", platform.minecraftVersion());
    }

    public static BetterMobFabricMod instance() {
        return instance;
    }

    public FabricPlatform platform() {
        return platform;
    }
}
