package online.tflives.tflcore.client.hud.widget;

import net.minecraft.client.Minecraft;
import online.tflives.tflcore.client.config.TflSettings;
import online.tflives.tflcore.client.config.WidgetPlacement;
import online.tflives.tflcore.client.hud.HudWidget;

import java.util.Locale;

/** Player coordinates, with streamer masking and an optional Nether conversion. */
public final class CoordinatesWidget extends HudWidget {
    public CoordinatesWidget() {
        super("coordinates", true, WidgetPlacement.BOTTOM_LEFT, 6, 6);
        setRefreshIntervalNanos(150_000_000L);
    }

    @Override
    protected void collect(Minecraft client, TflSettings settings) {
        if (client.player == null) return;
        if (settings.hud.streamerMode) {
            line("XYZ  ***  ***  ***");
            return;
        }
        double x = client.player.getX();
        double y = client.player.getY();
        double z = client.player.getZ();
        if (settings.hud.widgets.getOrDefault("coordinates", defaultPlacement()).compact) {
            line(String.format(Locale.ROOT, "%d %d %d",
                    (int) Math.floor(x), (int) Math.floor(y), (int) Math.floor(z)));
            return;
        }
        line(String.format(Locale.ROOT, "XYZ  %d  %d  %d",
                (int) Math.floor(x), (int) Math.floor(y), (int) Math.floor(z)));
        String dimension = client.level == null ? "" : client.level.dimension().identifier().toString();
        if (dimension.contains("nether")) {
            line(String.format(Locale.ROOT, "OW   %d  %d",
                    (int) Math.floor(x * 8), (int) Math.floor(z * 8)));
        }
    }
}
