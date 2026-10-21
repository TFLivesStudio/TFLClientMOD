package online.tflives.tflcore.client.hud.widget;

import net.minecraft.client.Minecraft;
import online.tflives.tflcore.client.config.TflSettings;
import online.tflives.tflcore.client.config.WidgetPlacement;
import online.tflives.tflcore.client.hud.HudWidget;

import java.util.Locale;

/** Chunk coordinates and dimension of the local player. */
public final class ChunkWidget extends HudWidget {
    public ChunkWidget() {
        super("chunk", false, WidgetPlacement.TOP_LEFT, 6, 182);
        setRefreshIntervalNanos(400_000_000L);
    }

    @Override
    protected void collect(Minecraft client, TflSettings settings) {
        if (client.player == null || client.level == null) return;
        int chunkX = client.player.blockPosition().getX() >> 4;
        int chunkZ = client.player.blockPosition().getZ() >> 4;
        line("CHUNK  " + chunkX + " / " + chunkZ);
        String dimension = client.level.dimension().identifier().toString();
        line(titleCase(dimension));
    }
}
