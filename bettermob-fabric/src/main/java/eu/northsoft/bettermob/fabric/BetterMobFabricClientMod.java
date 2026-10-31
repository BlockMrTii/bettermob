package eu.northsoft.bettermob.fabric;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Environment(EnvType.CLIENT)
public final class BetterMobFabricClientMod implements ClientModInitializer {
    public static final Logger LOGGER = LoggerFactory.getLogger("bettermob-client");

    @Override
    public void onInitializeClient() {
        LOGGER.info("BetterMob client initialized.");
    }
}
