package dev.theredstonee.trsclient.ornithe;

import dev.theredstonee.trsclient.core.module.HudModule;
import dev.theredstonee.trsclient.core.module.Module;
import dev.theredstonee.trsclient.core.module.Setting;
import dev.theredstonee.trsclient.core.ui.*;
import dev.theredstonee.trsclient.core.ui.menu.SettingsPanel;
import java.util.*;

/**
 * Small platform capability view over the SAME registry and settings controls, not a second module
 * stack.
 */
public final class PocMenu extends UiScreen {
  private final PocHost host;
  private final List<Module> modules;
  private final SettingsPanel panel = new SettingsPanel();
  private Module selected;
  private List<Setting> settings = List.of();
  private int scroll, maxScroll;

  public PocMenu(PocHost host) {
    this.host = host;
    modules =
        host.modules().registry.all().stream()
            .filter(m -> OrnitheClient.SUPPORTED.contains(m.id()))
            .toList();
    select(modules.getFirst());
  }

  private void select(Module m) {
    selected = m;
    scroll = 0;
    panel.reset();
    settings = m.settings().stream().filter(s -> supportedSetting(m, s)).toList();
  }

  static boolean supportedSetting(Module m, Setting s) {
    if (m instanceof HudModule)
      return Set.of("scale", "shadow", "background", "backgroundOpacity", "textColor")
          .contains(s.key());
    if (m.id().equals("zoom")) return Set.of("factor", "smooth").contains(s.key());
    if (m.id().equals("crosshair"))
      return Set.of("shape", "color", "size", "gap", "thickness", "outline").contains(s.key());
    return false;
  }

  @Override
  protected void draw(Canvas raw, int w, int h, int mx, int my, float dt) {
    Canvas c = FadeCanvas.of(raw, alpha());
    Theme t = Theme.get();
    c.fill(0, 0, w, h, 0xF00B1019);
    c.strongText("TFL Client", 12, 10, t.text);
    c.text("Ornithe 1.8.9 · Phase 1", 12, 25, t.textDim, false);
    int left = Math.min(126, Math.max(96, w / 3)), y = 46;
    for (Module m : modules) {
      int top = y;
      SelectionStyle.button(
          c,
          8,
          top,
          left - 12,
          20,
          m == selected,
          mx >= 8 && mx < left && my >= top && my < top + 20);
      c.text(c.clip(m.name(), left - 45), 14, top + 5, t.text, false);
      c.text(
          m.isEnabled() ? "ON" : "OFF",
          left - 30,
          top + 5,
          m.isEnabled() ? (m == selected ? t.text : t.accent) : t.textDim,
          false);
      hits.add(8, top, left - 42, 20, () -> select(m));
      hits.add(
          left - 34,
          top,
          28,
          20,
          () -> {
            m.toggle();
            host.save();
          });
      y += 23;
    }
    int x = left + 10, tw = w - x - 12, view = Math.max(20, h - 80);
    maxScroll = Math.max(0, panel.height(settings) - view);
    scroll = Math.min(scroll, maxScroll);
    c.strongText(c.clip(selected.name(), tw), x, 48, t.text);
    c.scissor(x, 64, w - 8, h - 32);
    hits.clip(x, 64, tw, view);
    panel.draw(c, hits, settings, x, 64 - scroll, tw, mx, my);
    hits.noClip();
    c.noScissor();
    button(c, 8, h - 27, Math.min(112, left - 12), "HUD Editor", mx, my, host::openHudEditor);
    button(c, w - 76, h - 27, 68, "Done", mx, my, this::requestClose);
    c.text("Experimental · 7 modules", left + 10, h - 21, t.textDim, false);
  }

  private void button(
      Canvas c, int x, int y, int w, String label, int mx, int my, Runnable action) {
    SelectionStyle.button(c, x, y, w, 20, false, mx >= x && mx < x + w && my >= y && my < y + 20);
    c.text(label, x + 7, y + 5, Theme.get().text, false);
    hits.add(x, y, w, 20, action);
  }

  @Override
  public boolean mouseScrolled(double x, double y, double amount) {
    scroll = Math.max(0, Math.min(maxScroll, scroll - (int) (amount * 24)));
    return true;
  }

  @Override
  public boolean keyPressed(int raw, UiKey key, boolean shift) {
    if (panel.typeKey(key)) return true;
    if (key == UiKey.ESCAPE) {
      if (!panel.collapse()) requestClose();
      return true;
    }
    return false;
  }

  @Override
  public boolean charTyped(char ch) {
    return panel.typeChar(ch);
  }

  @Override
  protected void onClosed() {
    host.save();
    host.closeScreen();
  }
}
