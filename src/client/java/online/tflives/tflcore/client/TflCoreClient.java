package online.tflives.tflcore.client;

import net.fabricmc.api.ClientModInitializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Client-only bootstrap for the Fabric edition of TFL Selection. */
public final class TflCoreClient implements ClientModInitializer {
    public static final String MOD_ID = "tflcore";
    private static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    @Override
    public void onInitializeClient() {
        LOGGER.info("TFL Core {} initialized (Minecraft 26.3, Fabric).", BuildInfo.VERSION);
        // Future modules must register here via isolated, opt-in feature services.
        // Never contact remote services or collect telemetry by default.
    }
}
