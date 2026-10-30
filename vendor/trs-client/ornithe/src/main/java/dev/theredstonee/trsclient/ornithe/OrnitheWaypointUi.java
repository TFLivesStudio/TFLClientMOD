package dev.theredstonee.trsclient.ornithe;

import dev.theredstonee.trsclient.core.ui.*;
import dev.theredstonee.trsclient.core.ui.menus.*;
import dev.theredstonee.trsclient.core.waypoint.Waypoint;

/** The Diorama waypoint fields, edited transactionally and bound to the world opened. */
final class OrnitheWaypointUi extends WindowUi {
  private final OrnitheClient c;
  private final String owner;
  private final Waypoint point;
  private final boolean fresh;
  private final Runnable close;
  private final TextInput[] fields = {
    new TextInput(32),
    new TextInput(12),
    new TextInput(12),
    new TextInput(12),
    new TextInput(32),
    new TextInput(2)
  };
  private String error = "", icon;
  private int color;
  private boolean visible;

  OrnitheWaypointUi(OrnitheClient c, Waypoint p, boolean fresh, Runnable close) {
    this(c, p, fresh, close, c.mapPlatform.worldKey());
  }

  OrnitheWaypointUi(OrnitheClient c, Waypoint p, boolean fresh, Runnable close, String worldKey) {
    this.c = c;
    owner = worldKey;
    point = p;
    this.fresh = fresh;
    this.close = close;
    color = p.color;
    icon = p.icon == null ? "" : p.icon;
    visible = p.visible;
    fields[0].setText(p.name);
    fields[1].setText("" + p.x);
    fields[2].setText("" + p.y);
    fields[3].setText("" + p.z);
    fields[4].setText(p.folder == null ? "" : p.folder);
    fields[5].setText(p.initials == null ? "" : p.initials);
    fields[0].setFocused(true);
  }

  protected int[] size(int w, int h) {
    return new int[] {Math.min(w - 16, 420), Math.min(h - 16, 310)};
  }

  protected String title() {
    return "Waypoint";
  }

  protected void playClick() {
    c.clickSound();
  }

  private void field(Canvas c, int x, int y, int w, String label, int index) {
    c.text(label, x, y, Theme.get().textDim, false);
    MenuSkin.textField(c, x, y + 11, w, 19, fields[index].focused(), false, true);
    c.text(c.clip(fields[index].text(), w - 8), x + 4, y + 16, Theme.get().text, false);
    hits.add(
        x,
        y + 11,
        w,
        19,
        () -> {
          for (int i = 0; i < fields.length; i++) fields[i].setFocused(i == index);
        });
  }

  protected void content(Canvas canvas, int x, int y, int w, int h, int mx, int my, float dt) {
    field(canvas, x, y, w - 70, dev.theredstonee.trsclient.core.i18n.I18n.tr("serverMaps.name"), 0);
    field(
        canvas,
        x + w - 64,
        y,
        64,
        dev.theredstonee.trsclient.core.i18n.I18n.tr("serverMaps.initials"),
        5);
    int cw = (w - 12) / 3;
    field(canvas, x, y + 37, cw, "X", 1);
    field(canvas, x + cw + 6, y + 37, cw, "Y", 2);
    field(canvas, x + 2 * (cw + 6), y + 37, cw, "Z", 3);
    field(
        canvas, x, y + 74, w, dev.theredstonee.trsclient.core.i18n.I18n.tr("serverMaps.folder"), 4);
    button(
        canvas,
        x,
        y + 111,
        72,
        22,
        dev.theredstonee.trsclient.core.i18n.I18n.tr("serverMaps.save"),
        true,
        true,
        mx,
        my,
        () -> save());
    if (!fresh)
      button(
          canvas,
          x + 78,
          y + 111,
          72,
          22,
          dev.theredstonee.trsclient.core.i18n.I18n.tr("serverMaps.delete"),
          false,
          true,
          mx,
          my,
          () -> {
            c.waypoints.remove(owner, point);
            c.mapPlatform.waypointsChanged(owner);
            requestClose();
          });
    button(
        canvas,
        x + 156,
        y + 111,
        72,
        22,
        dev.theredstonee.trsclient.core.i18n.I18n.tr("serverMaps.color"),
        false,
        true,
        mx,
        my,
        () -> {
          int[] palette = {
            0xe5484d, 0xf76b15, 0xffc53d, 0x46a758, 0x12a594, 0x0090ff, 0x3e63dd, 0x8e4ec6,
            0xd6409f, 0xffffff, 0xb0b4ba, 0x60646c, 0x8d6b4b, 0x99d52a, 0x7ce2fe, 0xf0c0d8
          };
          int next = 0;
          for (int i = 0; i < palette.length; i++)
            if (color == palette[i]) next = (i + 1) % palette.length;
          color = palette[next];
        });
    SelectionStyle.rounded(canvas, x + 234, y + 115, 14, 14, 4, color | 0xff000000);
    button(
        canvas,
        x,
        y + 139,
        Math.min(150, w / 2 - 3),
        22,
        "Icon: "
            + (icon.isEmpty()
                ? dev.theredstonee.trsclient.core.i18n.I18n.tr("serverMaps.initials")
                : icon.replace("diorama:", "")),
        false,
        true,
        mx,
        my,
        () -> {
          String[] icons = {
            "",
            "diorama:home",
            "diorama:bed",
            "diorama:farm",
            "diorama:animals",
            "diorama:villagers",
            "diorama:mine",
            "diorama:storage",
            "diorama:portal",
            "diorama:danger",
            "diorama:treasure"
          };
          int next = 0;
          for (int i = 0; i < icons.length; i++)
            if (icons[i].equals(icon)) next = (i + 1) % icons.length;
          icon = icons[next];
        });
    button(
        canvas,
        x + w / 2 + 3,
        y + 139,
        w / 2 - 3,
        22,
        visible
            ? dev.theredstonee.trsclient.core.i18n.I18n.tr("serverMaps.visibleYes")
            : dev.theredstonee.trsclient.core.i18n.I18n.tr("serverMaps.visibleNo"),
        false,
        true,
        mx,
        my,
        () -> visible = !visible);
    canvas.text(canvas.clip(error, w), x, y + h - 10, 0xffff9090, false);
  }

  private void save() {
    try {
      int wx = Integer.parseInt(fields[1].text()),
          wy = Integer.parseInt(fields[2].text()),
          wz = Integer.parseInt(fields[3].text());
      if (Math.abs((long) wx) > 30000000 || Math.abs((long) wz) > 30000000 || wy < 0 || wy > 255)
        throw new NumberFormatException();
      point.name = fields[0].text();
      point.initials = fields[5].text();
      point.folder = fields[4].text().trim();
      point.icon = icon;
      point.visible = visible;
      point.color = color;
      point.x = wx;
      point.y = wy;
      point.z = wz;
      if (fresh) c.waypoints.add(owner, point);
      else c.waypoints.touch();
      c.mapPlatform.waypointsChanged(owner);
      requestClose();
    } catch (NumberFormatException e) {
      error = dev.theredstonee.trsclient.core.i18n.I18n.tr("serverMaps.invalidCoordinates");
    }
  }

  public boolean charTyped(char ch) {
    for (TextInput f : fields) if (f.focused()) return f.type(ch);
    return false;
  }

  public boolean keyPressed(int raw, UiKey key, boolean shift) {
    if (key == UiKey.TAB) {
      int next = 0;
      for (int i = 0; i < fields.length; i++)
        if (fields[i].focused()) next = (i + (shift ? fields.length - 1 : 1)) % fields.length;
      for (int i = 0; i < fields.length; i++) fields[i].setFocused(i == next);
      return true;
    }
    for (TextInput f : fields) if (f.focused() && f.key(key)) return true;
    return super.keyPressed(raw, key, shift);
  }

  public boolean wantsTextInput() {
    return true;
  }

  protected void onClosed() {
    close.run();
  }
}
