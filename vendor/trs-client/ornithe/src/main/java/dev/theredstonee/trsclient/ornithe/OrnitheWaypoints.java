package dev.theredstonee.trsclient.ornithe;

import dev.theredstonee.trsclient.core.render.Projection;
import dev.theredstonee.trsclient.core.ui.*;
import dev.theredstonee.trsclient.core.waypoint.Waypoint;
import dev.theredstonee.trsclient.ornithe.capability.CapabilityRegistry.State;
import java.util.*;

/** Existing legacy projected waypoints, bounded and sampled outside render. */
public final class OrnitheWaypoints {
  private final OrnitheClient c;
  private static final int BEAM_HEIGHT = 48, BEAM_STEPS = 12;
  private final double[] point = new double[3], top = new double[3];
  private java.util.List<Waypoint> list = java.util.List.of();
  private boolean addDown, listDown;

  public OrnitheWaypoints(OrnitheClient c) {
    this.c = c;
    c.capabilities.connect(
        c.modules.waypoints,
        State.LIMITED,
        "Shared persistent points, death markers, add/list bindings and projected"
            + " names/distances/beams in first person. Third-person world projection is"
            + " intentionally omitted.",
        c.modules.waypoints.settings().stream().map(setting -> setting.key()).toList(),
        "OrnitheWaypoints.tick",
        "WaypointSaveUi",
        "WaypointShare",
        "WaypointStore",
        "Projection");
  }

  public void tick() {
    boolean active = c.capabilities.active(c.modules.waypoints) && c.mapPlatform.inWorld();
    boolean add = OrnitheKeys.isDown(c.modules.waypointAddKey.get()),
        open = OrnitheKeys.isDown(c.modules.waypointListKey.get());
    if (active && c.mc.screen == null) {
      if (add && !addDown) create();
      else if (open && !listDown) c.openWorldMap(null);
    }
    addDown = add;
    listDown = open;
    if (!active) {
      list = java.util.List.of();
      return;
    }
    var points = new java.util.ArrayList<Waypoint>();
    for (var p : c.waypoints.all(c.mapPlatform.worldKey()))
      if (p.visible && p.inDimension(c.mapPlatform.dimension()) && points.size() < 200)
        points.add(p);
    list = points;
  }

  private void create() {
    var parent = c.mc.screen;
    var p = c.mc.player;
    c.mc.openScreen(
        new CoreScreen(
            new WaypointSaveUi(
                new WaypointSaveUi.Host() {
                  public void closeScreen() {
                    c.mc.openScreen(parent);
                  }

                  public void playClick() {
                    c.clickSound();
                  }

                  public void notice(String text) {
                    c.mapPlatform.message(text);
                  }

                  public String paste() {
                    return net.minecraft.client.gui.screen.Screen.getClipboard();
                  }
                },
                (int) Math.floor(p.x),
                (int) Math.floor(p.y),
                (int) Math.floor(p.z),
                ""),
            false));
  }

  public void draw(Canvas g, int w, int h) {
    if (!c.capabilities.active(c.modules.waypoints)
        || c.mc.player == null
        || c.mc.options.perspective != 0
        || c.cinematic.detached()) return;
    var player = c.mc.player;
    double camX = player.x, camY = player.y + player.getEyeHeight(), camZ = player.z;
    float yaw = player.yaw, pitch = player.pitch;
    double fov = c.worldFov;
    double range = c.modules.waypointRange.get();

    for (int i = 0, n = list.size(); i < n; i++) {
      Waypoint waypoint = list.get(i);
      if (!waypoint.visible) continue;
      double distance = waypoint.distanceTo(camX, camY, camZ);
      if (range > 0 && distance > range) continue;
      double wx = waypoint.x + 0.5;
      double wy = waypoint.y + 0.5;
      double wz = waypoint.z + 0.5;
      if (!Projection.project(camX, camY, camZ, yaw, pitch, fov, w, h, wx, wy, wz, point)) continue;
      int x = (int) Math.round(point[0]);
      int y = (int) Math.round(point[1]);
      if (x < -40 || x > w + 40 || y < -40 || y > h + 40) continue;
      int color = 0xFF000000 | waypoint.color;

      if (c.modules.waypointBeam.get())
        drawBeam(g, camX, camY, camZ, yaw, pitch, fov, w, h, wx, wy, wz, color);

      // Markierung (kleines Quadrat) + Name
      g.fill(x - 2, y - 2, x + 3, y + 3, color);

      String label = waypoint.name;
      if (c.modules.waypointDistance.get()) label += "  " + Projection.distanceLabel(distance);
      int tw = g.textWidth(label);
      int tx = x - tw / 2;
      int ty = y - 14;
      g.fill(tx - 2, ty - 2, tx + tw + 2, ty + 9, 0x90000000);
      g.text(label, tx, ty, color, false);
    }
  }

  /** Lichtsäule als Kette kleiner Rechtecke (in der Projektion sind senkrechte Linien schräg). */
  private void drawBeam(
      Canvas g,
      double camX,
      double camY,
      double camZ,
      float yaw,
      float pitch,
      double fov,
      int w,
      int h,
      double wx,
      double wy,
      double wz,
      int color) {
    int lastX = Integer.MIN_VALUE;
    int lastY = 0;
    int beamColor = (color & 0xFFFFFF) | 0x90000000;
    for (int step = 0; step <= BEAM_STEPS; step++) {
      double y = wy + (double) BEAM_HEIGHT * step / BEAM_STEPS;
      if (!Projection.project(camX, camY, camZ, yaw, pitch, fov, w, h, wx, y, wz, top)) break;
      int px = (int) Math.round(top[0]);
      int py = (int) Math.round(top[1]);
      if (lastX != Integer.MIN_VALUE) line(g, lastX, lastY, px, py, beamColor);
      lastX = px;
      lastY = py;
    }
  }

  /** Dünne Linie aus Rechtecken (zwei Punkte, immer 2 Pixel breit). */
  private static void line(Canvas g, int x1, int y1, int x2, int y2, int color) {
    int dx = Math.abs(x2 - x1);
    int dy = Math.abs(y2 - y1);
    int steps = Math.max(dx, dy);
    if (steps <= 0) {
      g.fill(x1, y1, x1 + 2, y1 + 1, color);
      return;
    }
    if (steps > 400) return; // sehr nah dran – nicht zeichnen statt tausende Rechtecke
    for (int i = 0; i <= steps; i++) {
      int x = x1 + (x2 - x1) * i / steps;
      int y = y1 + (y2 - y1) * i / steps;
      g.fill(x, y, x + 2, y + 1, color);
    }
  }
}
