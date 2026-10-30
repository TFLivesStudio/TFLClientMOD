package dev.theredstonee.trsclient.core.map.session;

import dev.theredstonee.trsclient.core.ui.*;
import dev.theredstonee.trsclient.core.ui.menus.*;
import java.io.IOException;

/** Explicit selection is available when the server supplies no unambiguous world ID. */
public final class WorldCatalogUi extends WindowUi {
  private final WorldCatalog catalog;
  private final Runnable changed, close, importPrevious;
  private final TextInput name = new TextInput(64);
  private String error = "";
  private int scroll, maxScroll;

  public WorldCatalogUi(WorldCatalog catalog, Runnable changed, Runnable close) {
    this(catalog, changed, close, null);
  }

  public WorldCatalogUi(
      WorldCatalog catalog, Runnable changed, Runnable close, Runnable importPrevious) {
    this.importPrevious = importPrevious;
    this.catalog = catalog;
    this.changed = changed;
    this.close = close;
    name.setText(catalog.name());
    name.setFocused(true);
  }

  protected String title() {
    return dev.theredstonee.trsclient.core.i18n.I18n.tr("serverMaps.worlds");
  }

  protected void playClick() {}

  protected void content(Canvas c, int x, int y, int w, int h, int mx, int my, float dt) {
    c.text(
        c.clip(dev.theredstonee.trsclient.core.i18n.I18n.tr("serverMaps.hint"), w),
        x,
        y,
        Theme.get().textDim,
        false);
    MenuSkin.textField(c, x, y + 18, w - 142, 22, name.focused(), false, true);
    c.text(c.clip(name.text(), w - 150), x + 5, y + 25, Theme.get().text, false);
    hits.add(x, y + 18, w - 142, 22, () -> name.setFocused(true));
    button(
        c,
        x + w - 136,
        y + 18,
        76,
        22,
        dev.theredstonee.trsclient.core.i18n.I18n.tr("serverMaps.rename"),
        false,
        true,
        mx,
        my,
        () -> {
          try {
            catalog.rename(name.text());
          } catch (Exception e) {
            error = e.getMessage();
          }
        });
    button(
        c,
        x + w - 56,
        y + 18,
        56,
        22,
        dev.theredstonee.trsclient.core.i18n.I18n.tr("serverMaps.new"),
        false,
        true,
        mx,
        my,
        () -> {
          try {
            catalog.create();
            name.setText(catalog.name());
            changed.run();
          } catch (IOException e) {
            error = e.getMessage();
          }
        });
    int top = y + 50, area = Math.max(20, h - (importPrevious == null ? 76 : 102));
    c.scissor(x, top, x + w, top + area);
    hits.clip(x, top, w, area);
    int row = 0;
    for (WorldCatalog.Entry e : catalog.entries()) {
      int yy = top + row++ * 26 - scroll;
      if (yy + 22 < top || yy >= top + area) continue;
      button(
          c,
          x,
          yy,
          w - 4,
          22,
          (e.id.equals(catalog.activeId()) ? "✓ " : "") + e.name,
          false,
          true,
          mx,
          my,
          () -> {
            catalog.select(e.id);
            name.setText(catalog.name());
            changed.run();
          });
    }
    maxScroll = Math.max(0, catalog.entries().size() * 26 - area);
    scroll = Math.min(scroll, maxScroll);
    hits.noClip();
    c.noScissor();
    if (importPrevious != null)
      button(
          c,
          x,
          y + h - 43,
          w - 4,
          22,
          dev.theredstonee.trsclient.core.i18n.I18n.tr("serverMaps.importPrevious"),
          false,
          true,
          mx,
          my,
          importPrevious);
    c.text(c.clip(error, w), x, y + h - 14, 0xffff9090, false);
  }

  public boolean mouseScrolled(double x, double y, double d) {
    scroll = Math.max(0, Math.min(maxScroll, scroll - (int) (d * 26)));
    return true;
  }

  public boolean keyPressed(int raw, UiKey key, boolean shift) {
    return name.key(key) || super.keyPressed(raw, key, shift);
  }

  public boolean charTyped(char ch) {
    return name.type(ch);
  }

  public boolean wantsTextInput() {
    return true;
  }

  protected void onClosed() {
    close.run();
  }
}
