package online.tflives.tflcore.client.hud.widget;

import net.minecraft.client.Minecraft;
import online.tflives.tflcore.client.config.TflSettings;
import online.tflives.tflcore.client.config.WidgetPlacement;
import online.tflives.tflcore.client.hud.Diagnostics;
import online.tflives.tflcore.client.hud.HudWidget;

import java.util.Locale;

/** Average frame time plus a real p95 percentile from the bounded diagnostics buffer. */
public final class FrametimeWidget extends HudWidget {
    public FrametimeWidget() {
        super("frametime", false, WidgetPlacement.TOP_LEFT, 6, 28);
        setRefreshIntervalNanos(400_000_000L);
    }

    @Override
    protected void collect(Minecraft client, TflSettings settings) {
        if (Diagnostics.samples() < 8) {
            line("FRAME  --");
            return;
        }
        line(String.format(Locale.ROOT, "FRAME  %.1f ms", Diagnostics.averageMillis()));
        line(String.format(Locale.ROOT, "p95    %.1f ms", Diagnostics.percentileMillis(95)));
    }
}
