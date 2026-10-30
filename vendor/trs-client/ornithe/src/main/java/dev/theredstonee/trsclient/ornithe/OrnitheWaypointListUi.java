package dev.theredstonee.trsclient.ornithe;

import dev.theredstonee.trsclient.core.ui.*;
import dev.theredstonee.trsclient.core.ui.menus.*;
import dev.theredstonee.trsclient.core.waypoint.Waypoint;

/** Diorama-style per-world waypoints, folders, visibility and icon editing. */
final class OrnitheWaypointListUi extends WindowUi {
  private final OrnitheClient c;
  private final String owner;
  private final Runnable close;
  private int scroll;

  OrnitheWaypointListUi(OrnitheClient c, Runnable close) {
    this.c = c;
    owner = c.mapPlatform.worldKey();
    this.close = close;
  }

  protected String title() {
    return dev.theredstonee.trsclient.core.i18n.I18n.tr("serverMaps.waypoints");
  }

  protected void playClick() {
    c.clickSound();
  }

  protected void content(Canvas canvas, int x, int y, int w, int h, int mx, int my, float dt) {
    button(
        canvas,
        x,
        y,
        72,
        22,
        dev.theredstonee.trsclient.core.i18n.I18n.tr("serverMaps.new"),
        true,
        true,
        mx,
        my,
        () -> {
          var parent = c.mc.screen;
          c.mc.openScreen(
              new CoreScreen(
                  new OrnitheWaypointUi(
                      c,
                      new Waypoint(
                          "Waypoint",
                          (int) c.mapPlatform.x(),
                          (int) c.mapPlatform.y(),
                          (int) c.mapPlatform.z(),
                          c.mapPlatform.dimension(),
                          0x0090ff),
                      true,
                      () -> c.mc.openScreen(parent)),
                  false));
        });
    button(
        canvas,
        x + 78,
        y,
        116,
        22,
        dev.theredstonee.trsclient.core.i18n.I18n.tr("serverMaps.importPrevious"),
        false,
        !c.mc.isSingleplayer(),
        mx,
        my,
        () -> {
          var entry = c.mc.getCurrentServerEntry();
          if (entry == null) return;
          String old = dev.theredstonee.trsclient.core.waypoint.WaypointStore.serverKey(entry.ip);
          java.util.Set<java.util.UUID> ids = new java.util.HashSet<>();
          for (var p : c.waypoints.all(owner)) ids.add(p.id);
          for (var p : new java.util.ArrayList<>(c.waypoints.all(old)))
            if (!ids.contains(p.id)) {
              Waypoint copy =
                  new com.google.gson.Gson()
                      .fromJson(new com.google.gson.Gson().toJson(p), Waypoint.class);
              c.waypoints.add(owner, copy);
            }
          c.mapPlatform.waypointsChanged(owner);
        });
    int top = y + 29, area = Math.max(22, h - 30), yy = top - scroll;
    canvas.scissor(x, top, x + w, top + area);
    hits.clip(x, top, w, area);
    java.util.Map<String, java.util.List<Waypoint>> groups = new java.util.LinkedHashMap<>();
    for (Waypoint p : c.waypoints.all(owner))
      groups
          .computeIfAbsent(p.folder == null ? "" : p.folder, k -> new java.util.ArrayList<>())
          .add(p);
    for (var group : groups.entrySet()) {
      String folder = group.getKey();
      if (!folder.isEmpty()) {
        final boolean shown = !c.mapPlatform.hiddenFolder(owner, folder);
        button(
            canvas,
            x,
            yy,
            w - 4,
            21,
            (shown
                ? dev.theredstonee.trsclient.core.i18n.I18n.tr("serverMaps.hideFolder", folder)
                : dev.theredstonee.trsclient.core.i18n.I18n.tr("serverMaps.showFolder", folder)),
            false,
            true,
            mx,
            my,
            () -> c.mapPlatform.toggleFolder(owner, folder));
        yy += 25;
      }
      for (Waypoint point : group.getValue()) {
        final int rowY = yy;
        if (yy + 22 >= top && yy < top + area) {
          button(
              canvas,
              x,
              yy,
              w - 62,
              22,
              point.name,
              false,
              true,
              mx,
              my,
              () -> {
                var parent = c.mc.screen;
                c.mc.openScreen(
                    new CoreScreen(
                        new OrnitheWaypointUi(
                            c, point, false, () -> c.mc.openScreen(parent), owner),
                        false));
              });
          button(
              canvas,
              x + w - 57,
              yy,
              53,
              22,
              point.visible
                  ? dev.theredstonee.trsclient.core.i18n.I18n.tr("serverMaps.hide")
                  : dev.theredstonee.trsclient.core.i18n.I18n.tr("serverMaps.show"),
              false,
              true,
              mx,
              my,
              () -> {
                point.visible = !point.visible;
                c.waypoints.touch();
                c.mapPlatform.waypointsChanged(owner);
              });
        }
        yy += 26;
      }
    }
    hits.noClip();
    canvas.noScissor();
    maxScroll = Math.max(0, yy + scroll - top - area);
    scroll = Math.min(scroll, maxScroll);
  }

  private int maxScroll;

  public boolean mouseScrolled(double x, double y, double d) {
    scroll = Math.max(0, Math.min(maxScroll, scroll - (int) (d * 26)));
    return true;
  }

  protected void onClosed() {
    close.run();
  }
}
