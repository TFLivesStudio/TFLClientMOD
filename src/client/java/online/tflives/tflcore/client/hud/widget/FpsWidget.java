package online.tflives.tflcore.client.hud.widget;

import net.minecraft.client.Minecraft;
import online.tflives.tflcore.client.config.TflSettings;
import online.tflives.tflcore.client.config.WidgetPlacement;
import online.tflives.tflcore.client.hud.HudWidget;

/** Frames per second reported by the vanilla client. */
public final class FpsWidget extends HudWidget {
    public FpsWidget() {
        super("fps", true, WidgetPlacement.TOP_LEFT, 6, 6);
        setRefreshIntervalNanos(200_000_000L);
    }

    @Override
    protected void collect(Minecraft client, TflSettings settings) {
        line("FPS  " + client.getFps());
    }
}
