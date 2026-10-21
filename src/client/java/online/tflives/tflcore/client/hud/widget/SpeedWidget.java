package online.tflives.tflcore.client.hud.widget;

import net.minecraft.client.Minecraft;
import online.tflives.tflcore.client.config.TflSettings;
import online.tflives.tflcore.client.config.WidgetPlacement;
import online.tflives.tflcore.client.hud.HudWidget;

import java.util.Locale;

/** Horizontal movement speed in blocks per second. */
public final class SpeedWidget extends HudWidget {
    public SpeedWidget() {
        super("speed", false, WidgetPlacement.TOP_LEFT, 6, 116);
        setRefreshIntervalNanos(200_000_000L);
    }

    @Override
    protected void collect(Minecraft client, TflSettings settings) {
        if (client.player == null) return;
        double blocksPerSecond = client.player.getDeltaMovement().horizontalDistance() * 20.0;
        line(String.format(Locale.ROOT, "SPEED  %.1f b/s", blocksPerSecond));
    }
}
