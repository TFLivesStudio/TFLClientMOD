package online.tflives.tflcore.client.hud.widget;

import net.minecraft.client.Minecraft;
import online.tflives.tflcore.client.config.TflSettings;
import online.tflives.tflcore.client.config.WidgetPlacement;
import online.tflives.tflcore.client.hud.Diagnostics;
import online.tflives.tflcore.client.hud.HudWidget;

import java.util.Locale;

/** Compact TFL performance summary (real frame-time average and heap usage). */
public final class PerformanceWidget extends HudWidget {
    public PerformanceWidget() {
        super("performance", false, WidgetPlacement.TOP_RIGHT, 6, 72);
        setRefreshIntervalNanos(500_000_000L);
    }

    @Override
    protected void collect(Minecraft client, TflSettings settings) {
        line(String.format(Locale.ROOT, "FPS %d  |  %.1f ms", client.getFps(), Diagnostics.averageMillis()));
        line("RAM  " + Diagnostics.usedMemoryMb() + " / " + Diagnostics.maxMemoryMb() + " MB");
        line("p95  " + String.format(Locale.ROOT, "%.1f ms", Diagnostics.percentileMillis(95)));
    }
}
