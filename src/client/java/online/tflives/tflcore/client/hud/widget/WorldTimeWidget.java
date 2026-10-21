package online.tflives.tflcore.client.hud.widget;

import net.minecraft.client.Minecraft;
import online.tflives.tflcore.client.config.TflSettings;
import online.tflives.tflcore.client.config.WidgetPlacement;
import online.tflives.tflcore.client.hud.HudWidget;

import java.util.Locale;

/** World day counter and in-game clock derived from the world clock, not the system clock. */
public final class WorldTimeWidget extends HudWidget {
    public WorldTimeWidget() {
        super("worldtime", false, WidgetPlacement.TOP_LEFT, 6, 72);
        setRefreshIntervalNanos(500_000_000L);
    }

    @Override
    protected void collect(Minecraft client, TflSettings settings) {
        if (client.level == null) return;
        long clock = client.level.getDefaultClockTime();
        long day = clock / 24_000L + 1L;
        long ticks = Math.floorMod(clock, 24_000L);
        long hours = (ticks / 1_000L + 6L) % 24L;
        long minutes = (ticks % 1_000L) * 60L / 1_000L;
        line(String.format(Locale.ROOT, "DAY %d  %02d:%02d", day, hours, minutes));
    }
}
