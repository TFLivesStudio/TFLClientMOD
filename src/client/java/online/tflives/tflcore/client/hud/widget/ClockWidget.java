package online.tflives.tflcore.client.hud.widget;

import net.minecraft.client.Minecraft;
import online.tflives.tflcore.client.config.TflSettings;
import online.tflives.tflcore.client.config.WidgetPlacement;
import online.tflives.tflcore.client.hud.HudWidget;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;

/** Local device clock (explicitly labelled so it is not confused with world time). */
public final class ClockWidget extends HudWidget {
    private static final DateTimeFormatter FORMAT = DateTimeFormatter.ofPattern("HH:mm");

    public ClockWidget() {
        super("clock", false, WidgetPlacement.TOP_LEFT, 6, 50);
        setRefreshIntervalNanos(1_000_000_000L);
    }

    @Override
    protected void collect(Minecraft client, TflSettings settings) {
        line("LOCAL  " + LocalTime.now().format(FORMAT));
    }
}
