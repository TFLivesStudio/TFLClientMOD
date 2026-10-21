package online.tflives.tflcore.client.hud.widget;

import net.minecraft.client.Minecraft;
import online.tflives.tflcore.client.config.TflSettings;
import online.tflives.tflcore.client.config.WidgetPlacement;
import online.tflives.tflcore.client.hud.HudWidget;

/** Current biome id, resolved from the client's loaded chunk data. */
public final class BiomeWidget extends HudWidget {
    public BiomeWidget() {
        super("biome", false, WidgetPlacement.TOP_LEFT, 6, 94);
        setRefreshIntervalNanos(1_500_000_000L);
    }

    @Override
    protected void collect(Minecraft client, TflSettings settings) {
        if (client.level == null || client.player == null) return;
        String biome = client.level.getBiome(client.player.blockPosition()).getRegisteredName();
        line("BIOME  " + titleCase(biome));
    }
}
