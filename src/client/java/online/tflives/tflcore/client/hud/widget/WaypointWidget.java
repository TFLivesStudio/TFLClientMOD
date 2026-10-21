package online.tflives.tflcore.client.hud.widget;

import net.minecraft.client.Minecraft;
import online.tflives.tflcore.client.config.TflSettings;
import online.tflives.tflcore.client.config.Waypoint;
import online.tflives.tflcore.client.config.WidgetPlacement;
import online.tflives.tflcore.client.hud.HudWidget;
import online.tflives.tflcore.client.survival.WaypointKeys;

import java.util.List;
import java.util.Locale;

/** Compass-style guidance towards the selected or nearest personal waypoint. */
public final class WaypointWidget extends HudWidget {
    public WaypointWidget() {
        super("waypoint", false, WidgetPlacement.TOP_LEFT, 6, 204);
        setRefreshIntervalNanos(200_000_000L);
    }

    @Override
    protected void collect(Minecraft client, TflSettings settings) {
        if (client.player == null) return;
        String world = WaypointKeys.world(client);
        String dimension = WaypointKeys.dimension(client);
        List<Waypoint> available = settings.waypointsFor(world, dimension);
        if (available.isEmpty()) {
            line("WAYPOINT  \u2014");
            return;
        }
        Waypoint target = null;
        int index = settings.survival.activeWaypoint;
        if (index >= 0 && index < available.size()) {
            target = available.get(index);
        }
        if (target == null) {
            double best = Double.MAX_VALUE;
            for (Waypoint waypoint : available) {
                if (!waypoint.enabled) continue;
                double distance = waypoint.horizontalDistanceTo(client.player.getX(), client.player.getZ());
                if (distance < best) {
                    best = distance;
                    target = waypoint;
                }
            }
        }
        if (target == null) {
            line("WAYPOINT  \u2014");
            return;
        }
        double px = client.player.getX();
        double pz = client.player.getZ();
        double distance = target.horizontalDistanceTo(px, pz);
        line(TflTrim.trim(target.name, 18));
        double bearing = Math.toDegrees(Math.atan2(-(target.x - px), target.z - pz));
        line(String.format(Locale.ROOT, "%s %.0fm", cardinal(bearing), distance));

        double relative = Math.floorMod((long) Math.round(bearing - client.player.getYRot()) + 180L, 360L) - 180L;
        String arrow;
        if (Math.abs(relative) <= 22.5) {
            arrow = "\u2191";
        } else if (Math.abs(relative) >= 157.5) {
            arrow = "\u2193";
        } else if (relative > 0) {
            arrow = relative < 90 ? "\u2197" : "\u2192";
        } else {
            arrow = relative > -90 ? "\u2196" : "\u2190";
        }
        line("TURN  " + arrow + " " + Math.round(relative) + "\u00B0");
    }

    private static String cardinal(double bearing) {
        double normalized = (bearing % 360 + 360) % 360;
        String[] names = {"S", "SW", "W", "NW", "N", "NE", "E", "SE"};
        int index = (int) Math.round(normalized / 45.0) % 8;
        return names[index];
    }

    private static final class TflTrim {
        static String trim(String value, int max) {
            if (value == null) return "";
            return value.length() <= max ? value : value.substring(0, max);
        }
    }
}
