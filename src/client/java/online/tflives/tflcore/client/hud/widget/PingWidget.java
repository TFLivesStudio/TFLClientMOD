package online.tflives.tflcore.client.hud.widget;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.client.multiplayer.PlayerInfo;
import online.tflives.tflcore.client.config.TflSettings;
import online.tflives.tflcore.client.config.WidgetPlacement;
import online.tflives.tflcore.client.hud.HudWidget;

/** Ping of the local player's own entry; shows N/A when no multiplayer connection exists. */
public final class PingWidget extends HudWidget {
    public PingWidget() {
        super("ping", false, WidgetPlacement.TOP_RIGHT, 6, 28);
        setRefreshIntervalNanos(500_000_000L);
    }

    @Override
    protected void collect(Minecraft client, TflSettings settings) {
        if (settings.hud.streamerMode) {
            line("PING  ***");
            return;
        }
        if (client.getConnection() == null || client.player == null) {
            line("PING  N/A");
            return;
        }
        ClientPacketListener connection = client.getConnection();
        PlayerInfo info = connection.getPlayerInfo(client.player.getUUID());
        if (info == null) {
            line("PING  N/A");
            return;
        }
        line("PING  " + info.getLatency() + " ms");
    }
}
