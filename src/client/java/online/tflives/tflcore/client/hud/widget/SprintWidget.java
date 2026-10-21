package online.tflives.tflcore.client.hud.widget;

import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import online.tflives.tflcore.client.config.TflSettings;
import online.tflives.tflcore.client.config.WidgetPlacement;
import online.tflives.tflcore.client.hud.HudWidget;

/** Sprint / sneak / walk state. */
public final class SprintWidget extends HudWidget {
    public SprintWidget() {
        super("sprint", false, WidgetPlacement.TOP_RIGHT, 6, 50);
        setRefreshIntervalNanos(100_000_000L);
    }

    @Override
    protected void collect(Minecraft client, TflSettings settings) {
        LocalPlayer player = client.player;
        if (player == null) return;
        String state;
        if (player.isCrouching()) {
            state = "SNEAK";
        } else if (player.isSprinting()) {
            state = "SPRINT";
        } else {
            state = "WALK";
        }
        line("MOVE  " + state);
    }
}
