package online.tflives.tflcore.client.hud;

import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.hud.VanillaHudElements;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.resources.Identifier;
import online.tflives.tflcore.client.config.SettingsStore;
import online.tflives.tflcore.client.config.TflSettings;
import online.tflives.tflcore.client.ui.TflTheme;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

/**
 * Small HUD compositor: data is sampled at most four times per second, not every frame.
 * Rendering remains at full refresh rate; when toggles change we refresh immediately.
 */
public final class TflHud {
    private static final DateTimeFormatter CLOCK_FORMAT = DateTimeFormatter.ofPattern("HH:mm");
    private static final String[] LINES = new String[3];
    private static final long SAMPLE_INTERVAL_NS = 250_000_000L;
    private static int lineCount;
    private static int maxWidth;
    private static int previousMask = -1;
    private static long lastSample;

    private TflHud() { }

    public static void register(SettingsStore store) {
        HudElementRegistry.attachElementBefore(VanillaHudElements.CHAT,
                Identifier.fromNamespaceAndPath("tflcore", "status_hud"),
                (graphics, delta) -> extract(graphics, store));
    }

    private static void extract(GuiGraphicsExtractor graphics, SettingsStore store) {
        Minecraft minecraft = Minecraft.getInstance();
        TflSettings settings = store.get();
        if (minecraft.player == null || !settings.hudEnabled) {
            previousMask = -1;
            return;
        }

        int mask = (settings.showFps ? 1 : 0)
                | (settings.showCoordinates ? 2 : 0)
                | (settings.showClock ? 4 : 0);
        if (mask == 0) return;

        long now = System.nanoTime();
        if (mask != previousMask || now - lastSample >= SAMPLE_INTERVAL_NS) {
            previousMask = mask;
            lastSample = now;
            lineCount = 0;
            maxWidth = 0;
            if (settings.showFps) LINES[lineCount++] = "FPS  " + minecraft.getFps();
            if (settings.showCoordinates) {
                LINES[lineCount++] = String.format(Locale.ROOT, "XYZ  %.0f / %.0f / %.0f",
                        minecraft.player.getX(), minecraft.player.getY(), minecraft.player.getZ());
            }
            if (settings.showClock) LINES[lineCount++] = "TIME  " + LocalTime.now().format(CLOCK_FORMAT);
            for (int i = 0; i < lineCount; i++) {
                maxWidth = Math.max(maxWidth, minecraft.font.width(LINES[i]));
            }
        }

        TflTheme theme = TflTheme.from(settings);
        int x = Math.min(settings.hudX, Math.max(0, graphics.guiWidth() - maxWidth - 18));
        int y = Math.min(settings.hudY, Math.max(0, graphics.guiHeight() - lineCount * 13 - 12));
        graphics.fill(x, y, x + maxWidth + 16, y + lineCount * 13 + 10, theme.panel());
        graphics.fill(x, y, x + 2, y + lineCount * 13 + 10, theme.accent());
        for (int i = 0; i < lineCount; i++) {
            graphics.text(minecraft.font, LINES[i], x + 8, y + 6 + i * 13, theme.text(), true);
        }
    }
}
