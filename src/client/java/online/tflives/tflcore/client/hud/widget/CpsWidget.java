package online.tflives.tflcore.client.hud.widget;

import net.minecraft.client.Minecraft;
import online.tflives.tflcore.client.config.TflSettings;
import online.tflives.tflcore.client.config.WidgetPlacement;
import online.tflives.tflcore.client.hud.HudWidget;
import online.tflives.tflcore.client.hud.InputTracker;

/** Clicks per second, measured from the local player's own mouse input. */
public final class CpsWidget extends HudWidget {
    public CpsWidget() {
        super("cps", false, WidgetPlacement.TOP_RIGHT, 6, 6);
        setRefreshIntervalNanos(100_000_000L);
    }

    @Override
    protected void collect(Minecraft client, TflSettings settings) {
        line("CPS  " + InputTracker.leftCps() + " / " + InputTracker.rightCps());
    }
}
