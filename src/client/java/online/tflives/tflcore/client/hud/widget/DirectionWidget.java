package online.tflives.tflcore.client.hud.widget;

import net.minecraft.client.Minecraft;
import online.tflives.tflcore.client.config.TflSettings;
import online.tflives.tflcore.client.config.WidgetPlacement;
import online.tflives.tflcore.client.hud.HudWidget;

import java.util.Locale;

/** Facing direction plus yaw in degrees. */
public final class DirectionWidget extends HudWidget {
    public DirectionWidget() {
        super("direction", false, WidgetPlacement.BOTTOM_LEFT, 6, 28);
        setRefreshIntervalNanos(150_000_000L);
    }

    @Override
    protected void collect(Minecraft client, TflSettings settings) {
        if (client.player == null) return;
        int yaw = Math.floorMod(Math.round(client.player.getYRot()), 360);
        String facing = client.player.getDirection().getName().toUpperCase(Locale.ROOT);
        line(facing + "  " + yaw + "\u00B0");
    }
}
