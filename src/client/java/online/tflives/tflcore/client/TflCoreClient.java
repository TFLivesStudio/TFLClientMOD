package online.tflives.tflcore.client;

import com.mojang.blaze3d.platform.InputConstants;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.resources.Identifier;
import online.tflives.tflcore.client.config.SettingsStore;
import online.tflives.tflcore.client.hud.TflHud;
import online.tflives.tflcore.client.ui.TflMainMenuScreen;
import online.tflives.tflcore.client.ui.TflSettingsScreen;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Client-only bootstrap for the Fabric edition of TFL Selection. */
public final class TflCoreClient implements ClientModInitializer {
    public static final String MOD_ID = "tflcore";
    private static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);
    private static SettingsStore settings;

    public static SettingsStore settings() {
        if (settings == null) throw new IllegalStateException("TFL Core has not initialized yet");
        return settings;
    }

    @Override
    public void onInitializeClient() {
        settings = new SettingsStore(FabricLoader.getInstance().getConfigDir().resolve("tflcore.json"));

        KeyMapping.Category category = KeyMapping.Category.register(
                Identifier.fromNamespaceAndPath(MOD_ID, "controls"));
        KeyMapping settingsKey = KeyMappingHelper.registerKeyMapping(
                new KeyMapping("key.tflcore.settings",
                        InputConstants.Type.KEYBOARD, InputConstants.KEY_RSHIFT, category));

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            while (settingsKey.consumeClick()) {
                if (!(client.gui.screen() instanceof TflSettingsScreen)) {
                    client.gui.setScreen(new TflSettingsScreen(client.gui.screen()));
                }
            }
            if (client.gui.screen() instanceof TitleScreen && settings().get().customMainMenu) {
                client.gui.setScreen(new TflMainMenuScreen());
            }
        });

        TflHud.register(settings);
        LOGGER.info("TFL Core {} initialized (Minecraft 26.3, Fabric).", BuildInfo.VERSION);
    }
}
