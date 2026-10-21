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
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/** First low-cost HUD layer. Every element is independently switchable. */
public final class TflHud {
    private static final DateTimeFormatter CLOCK_FORMAT = DateTimeFormatter.ofPattern("HH:mm");
    private TflHud() { }

    public static void register(SettingsStore store) {
        HudElementRegistry.attachElementBefore(VanillaHudElements.CHAT,
                Identifier.fromNamespaceAndPath("tflcore", "status_hud"),
                (graphics, delta) -> extract(graphics, store));
    }

    private static void extract(GuiGraphicsExtractor graphics, SettingsStore store) {
        Minecraft minecraft = Minecraft.getInstance();
        TflSettings settings = store.get();
        if (minecraft.player == null || !settings.hudEnabled) return;

        List<String> lines = new ArrayList<>(3);
        if (settings.showFps) lines.add("FPS  " + minecraft.getFps());
        if (settings.showCoordinates) {
            lines.add(String.format(Locale.ROOT, "XYZ  %.0f / %.0f / %.0f",
                    minecraft.player.getX(), minecraft.player.getY(), minecraft.player.getZ()));
        }
        if (settings.showClock) lines.add("TIME  " + LocalTime.now().format(CLOCK_FORMAT));
        if (lines.isEmpty()) return;

        TflTheme palette = TflTheme.from(settings);
        int maxWidth = 0;
        for (String line : lines) maxWidth = Math.max(maxWidth, minecraft.font.width(line));
        int x = Math.min(settings.hudX, Math.max(0, graphics.guiWidth() - maxWidth - 18));
        int y = Math.min(settings.hudY, Math.max(0, graphics.guiHeight() - lines.size() * 13 - 12));
        graphics.fill(x, y, x + maxWidth + 16, y + lines.size() * 13 + 10, palette.panel());
        graphics.fill(x, y, x + 2, y + lines.size() * 13 + 10, palette.accent());
        for (int i = 0; i < lines.size(); i++) {
            graphics.text(minecraft.font, lines.get(i), x + 8, y + 6 + i * 13, palette.text(), true);
        }
    }
}
