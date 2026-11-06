package dev.theredstonee.trsclient.ornithe;

import dev.diorama.Projection;
import dev.theredstonee.trsclient.core.map.QuarterTurn;
import dev.theredstonee.trsclient.core.i18n.I18n;
import dev.theredstonee.trsclient.core.ui.*;
import dev.theredstonee.trsclient.core.waypoint.Waypoint;

/** Native TFL shell for the ported Diorama renderer. Coordinates use its oblique projection. */
public final class OrnitheDioramaUi extends UiScreen {
  private final OrnitheClient c;
  private final Runnable close;
  private double x, y, z, zoom = 8, lastX, lastY;
  private int width, height;
  private boolean follow = true;
  private int layer = dev.diorama.bake.TileKey.SURFACE;

  public OrnitheDioramaUi(OrnitheClient c, Runnable close) {
    this.c = c;
    this.close = close;
    x = c.mapPlatform.x();
    y = c.mapPlatform.y();
    z = c.mapPlatform.z();
  }

  protected void draw(Canvas canvas, int w, int h, int mx, int my, float dt) {
    width = w;
    height = h;
    if (follow) {
      x = c.mapPlatform.x();
      y = c.mapPlatform.y();
      z = c.mapPlatform.z();
    }
    double scale = c.window().getScale();
    double p = zoom / scale;
    c.diorama.draw(canvas, 0, 0, w, h, x, y, z, p, layer);
    Theme t = Theme.get();
    if (c.modules.worldMapWaypoints.get())
      for (Waypoint point :
          c.waypoints.visible(c.mapPlatform.worldKey(), c.mapPlatform.dimension())) {
        if (c.mapPlatform.hiddenFolder(point.folder)) continue;
        int q = c.diorama.rotation();
        int
            px =
                (int)
                    Math.round(
                        w / 2 + (QuarterTurn.u(point.x, point.z, q) - QuarterTurn.u(x, z, q)) * p),
            py =
                (int)
                    Math.round(
                        h / 2
                            + (Projection.v(point.y, QuarterTurn.depth(point.x, point.z, q))
                                    - Projection.v(y, QuarterTurn.depth(x, z, q)))
                                * p);
        if (px < 0 || py < 0 || px >= w || py >= h) continue;
        hits.add(
            px - 10,
            py - 24,
            Math.max(24, canvas.textWidth(point.name) + 24),
            28,
            () -> edit(point, false));
      }
    SelectionStyle.panel(canvas,8,8,w-16,26,Theme.get().surfaceHigh,t.border);
    canvas.strongText("TFL Maps",17,17,t.text);
    Toolbar toolbar=new Toolbar(canvas,hits,12,40,w-24,mx,my);
    toolbar.action(I18n.tr("overhaul.map.center"),()->follow=true);
    toolbar.action("−",()->zoom=Math.max(.125,zoom/1.4));
    toolbar.action("+",()->zoom=Math.min(16,zoom*1.4));
    toolbar.action(I18n.tr(layer==dev.diorama.bake.TileKey.SURFACE?"overhaul.map.surface":"overhaul.map.caves"),this::toggleLayer);
    if(layer!=dev.diorama.bake.TileKey.SURFACE) {
      toolbar.action("− Y",()->layer=Math.max(8,layer-8));
      toolbar.action("+ Y",()->layer=Math.min(255,layer+8));
    }
    toolbar.action(I18n.tr("serverMaps.rotate"),()->{
      try {c.mapPlatform.catalog().rotate(1);}
      catch(java.io.IOException failure){c.mapPlatform.message(failure.getMessage());}
    });
    if(!c.mc.isSingleplayer())toolbar.action(I18n.tr("serverMaps.worlds"),()->{
      var parent=c.mc.screen;
      c.mc.openScreen(new CoreScreen(new dev.theredstonee.trsclient.core.map.session.WorldCatalogUi(
          c.mapPlatform.catalog(),c.mapPlatform::selectedWorld,()->c.mc.openScreen(parent)),false));
    });
    toolbar.action(I18n.tr("serverMaps.waypoints"),()->{
      var parent=c.mc.screen;
      c.mc.openScreen(new CoreScreen(new OrnitheWaypointListUi(c,()->c.mc.openScreen(parent)),false));
    });
    toolbar.action(I18n.tr("overhaul.settings"),()->c.mc.openScreen(new CoreScreen(
        new dev.theredstonee.trsclient.core.ui.menu.ModMenu(new OrnitheMenuHost(c,c.mc.screen)).select(c.modules.worldMap),false)));
    toolbar.action(I18n.tr("common.done"),this::requestClose);
    int q = c.diorama.rotation();
    double viewU = QuarterTurn.u(x, z, q) + (mx - w / 2) / p,
        viewD = Projection.zAt(Projection.v(y, QuarterTurn.depth(x, z, q)) + (my - h / 2) / p, y);
    double worldX = QuarterTurn.x(viewU, viewD, q), worldZ = QuarterTurn.z(viewU, viewD, q);
    String status =
        String.format(
            java.util.Locale.ROOT,
            I18n.tr("overhaul.map.status"),
            worldX,
            worldZ,
            zoom,
            c.diorama.snapshotCount());
    SelectionStyle.panel(
        canvas,
        8,
        h - 24,
        Math.min(w - 16, canvas.textWidth(status) + 16),
        18,
        t.surfaceHigh,
        t.border);
    canvas.text(canvas.clip(status, w - 32), 16, h - 19, t.text, false);
    if (!c.diorama.failure.isEmpty())
      canvas.text(canvas.clip(c.diorama.failure, w - 20), 10, h - 38, 0xFFFFA0A0, true);
  }


  public boolean mouseClicked(double mx, double my, int button) {
    if (super.mouseClicked(mx, my, button)) return true;
    lastX = mx;
    lastY = my;
    if (button == 1) {
      double p = zoom / c.window().getScale();
      int q = c.diorama.rotation();
      double u = QuarterTurn.u(x, z, q) + (mx - width / 2) / p,
          v = Projection.v(y, QuarterTurn.depth(x, z, q)) + (my - height / 2) / p;
      var picked = c.diorama.pick(u, v, layer);
      if (picked != null) {
        Waypoint point =
            new Waypoint(
                "Waypoint",
                picked.getX(),
                picked.getY(),
                picked.getZ(),
                c.mapPlatform.dimension(),
                0x3B82F6);
        edit(point, true);
      }
      return true;
    }
    return button == 0;
  }

  public boolean mouseDragged(double mx, double my, int button) {
    if (button != 0) return false;
    double p = zoom / c.window().getScale();
    int q = c.diorama.rotation();
    double du = (mx - lastX) / p, dd = (my - lastY) / p / Projection.sin();
    x -= QuarterTurn.x(du, dd, q);
    z -= QuarterTurn.z(du, dd, q);
    lastX = mx;
    lastY = my;
    follow = false;
    return true;
  }

  public boolean mouseScrolled(double mx, double my, double delta) {
    double old = zoom / c.window().getScale(),
        next = Math.max(.125, Math.min(16, zoom * Math.pow(1.2, delta))) / c.window().getScale();
    int q = c.diorama.rotation();
    double anchorU = QuarterTurn.u(x, z, q) + (mx - width / 2) / old,
        anchorV = Projection.v(y, QuarterTurn.depth(x, z, q)) + (my - height / 2) / old;
    zoom = next * c.window().getScale();
    double u = anchorU - (mx - width / 2) / next,
        d = Projection.zAt(anchorV - (my - height / 2) / next, y);
    x = QuarterTurn.x(u, d, q);
    z = QuarterTurn.z(u, d, q);
    follow = false;
    return true;
  }

  public boolean keyPressed(int raw, UiKey key, boolean shift) {
    if(key==UiKey.TAB)return hits.focusNext(shift);
    if(key==UiKey.ENTER)return hits.activateFocused();
    if (key == UiKey.ESCAPE || raw == c.mapKey.getKeyCode()) {
      requestClose();
      return true;
    }
    if (raw == org.lwjgl.input.Keyboard.KEY_C) {
      toggleLayer();
      return true;
    }
    if (raw == org.lwjgl.input.Keyboard.KEY_PRIOR && layer != dev.diorama.bake.TileKey.SURFACE) {
      layer = Math.min(255, layer + 8);
      return true;
    }
    if (raw == org.lwjgl.input.Keyboard.KEY_NEXT && layer != dev.diorama.bake.TileKey.SURFACE) {
      layer = Math.max(8, layer - 8);
      return true;
    }
    if (raw == org.lwjgl.input.Keyboard.KEY_SPACE) {
      follow = true;
      return true;
    }
    return false;
  }

  private void toggleLayer() {
    if (!c.map.fairPlay().caveAllowed(c.mapPlatform.dimension().equals("dim-1"))) return;
    layer =
        layer == dev.diorama.bake.TileKey.SURFACE
            ? Math.min(255, (int) y + 16)
            : dev.diorama.bake.TileKey.SURFACE;
  }

  private void edit(Waypoint point, boolean fresh) {
    var parent = c.mc.screen;
    c.mc.openScreen(
        new CoreScreen(
            new OrnitheWaypointUi(c, point, fresh, () -> c.mc.openScreen(parent)), false));
  }

  protected void onClosed() {
    close.run();
  }

  public boolean pausesGame() {
    return false;
  }
}
