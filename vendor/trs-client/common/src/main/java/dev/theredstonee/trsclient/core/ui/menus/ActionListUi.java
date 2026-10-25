package dev.theredstonee.trsclient.core.ui.menus;

import dev.theredstonee.trsclient.core.ui.*;
import dev.theredstonee.trsclient.core.ui.menu.MenuAction;
import java.util.List;

/**
 * Shared action window for platform navigation and diagnostics; all controls use the common design
 * system.
 */
public final class ActionListUi extends WindowUi {
  private final String label;
  private String description = "";
  private final List<MenuAction> actions;
  private final Runnable close, click;
  private int scroll, maxScroll;

  public ActionListUi(String label, List<MenuAction> actions, Runnable close, Runnable click) {
    this.label = label;
    this.actions = actions;
    this.close = close;
    this.click = click;
  }

  public ActionListUi(String label, String description, Runnable close, Runnable click) {
    this(label, java.util.Collections.<MenuAction>emptyList(), close, click);
    this.description = description;
  }

  protected String title() {
    return label;
  }

  protected void playClick() {
    click.run();
  }

  protected void content(Canvas c, int x, int y, int w, int h, int mx, int my, float dt) {
    int row = 25;
    maxScroll = Math.max(0, actions.size() * row - h);
    scroll = Math.min(scroll, maxScroll);
    c.scissor(x, y, x + w, y + h);
    hits.clip(x, y, w, h);
    if (!description.isEmpty()) {
      List<String> lines = Paint.wrap(c, description, w - 16);
      maxScroll = Math.max(0, lines.size() * 13 + 10 - h);
      scroll = Math.min(scroll, maxScroll);
      for (int i = 0; i < lines.size(); i++)
        c.text(lines.get(i), x + 7, y + 5 + i * 13 - scroll, Theme.get().text, false);
    }

    for (int i = 0; i < actions.size(); i++) {
      MenuAction action = actions.get(i);
      int top = y + i * row - scroll;
      if (top + 22 < y || top > y + h) continue;
      MenuSkin.button(
          c, x, top, w - 4, 22, mx >= x && mx < x + w && my >= top && my < top + 22, true);
      c.text(c.clip(action.label(), w - 18), x + 7, top + 6, Theme.get().text, false);
      hits.add(
          x,
          top,
          w - 4,
          22,
          () -> {
            playClick();
            action.run();
          });
    }
    hits.noClip();
    c.noScissor();
  }

  public boolean mouseScrolled(double x, double y, double delta) {
    scroll = Math.max(0, Math.min(maxScroll, scroll - (int) (delta * 25)));
    return true;
  }

  protected void onClosed() {
    close.run();
  }
}
