package online.tflives.tflcore.client.survival;

import net.minecraft.client.Minecraft;
import net.minecraft.server.MinecraftServer;

/** Stable, local identifiers used to scope waypoints to a world/server and dimension. */
public final class WaypointKeys {
    private WaypointKeys() {
    }

    public static String world(Minecraft client) {
        if (client == null) return "";
        if (client.getCurrentServer() != null && client.getCurrentServer().ip != null) {
            return "server:" + client.getCurrentServer().ip;
        }
        MinecraftServer server = client.getSingleplayerServer();
        if (server != null) {
            try {
                String name = server.getWorldData().getLevelName();
                if (name != null && !name.isBlank()) {
                    return "sp:" + name;
                }
            } catch (RuntimeException ignored) {
                // fall through to the generic key
            }
        }
        return "singleplayer";
    }

    public static String dimension(Minecraft client) {
        if (client == null || client.level == null) return "";
        return client.level.dimension().identifier().toString();
    }
}
