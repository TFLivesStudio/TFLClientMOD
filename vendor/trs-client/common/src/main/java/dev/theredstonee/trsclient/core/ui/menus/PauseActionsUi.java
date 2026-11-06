package dev.theredstonee.trsclient.core.ui.menus;

import dev.theredstonee.trsclient.core.ui.*;
import dev.theredstonee.trsclient.core.ui.menu.MenuAction;
import java.util.ArrayList;
import java.util.List;

/** Six primary actions at the left; secondary native actions in the upper-right corner. */
public final class PauseActionsUi extends UiScreen {
  private final List<MenuAction> primary, secondary;
  private final Runnable close, click;
  private int focus = -1;
  private final float[] hover;

  public PauseActionsUi(
      String title,
      List<MenuAction> primary,
      List<MenuAction> secondary,
      Runnable close,
      Runnable click) {
    this.primary = new ArrayList<MenuAction>(primary);
    this.hover=new float[primary.size()];
    this.secondary = new ArrayList<MenuAction>(secondary);
    this.close = close;
    this.click = click;
  }

  public int primaryCount() {
    return primary.size();
  }

  public int secondaryCount() {
    return secondary.size();
  }

  @Override
  protected void draw(Canvas raw, int width, int height, int mx, int my, float dt) {
    Canvas c = FadeCanvas.of(raw, alpha());
    Theme t = Theme.get();
    int x = Math.max(12, Math.min(40, width / 16));
    int w = Math.min(200, width / 2 - x - 12);
    int row = Math.max(18, Math.min(35, (height - 54) / Math.max(1, primary.size())));
    int top = Math.max(30, (height - primary.size() * row) / 2);
    c.push();
    c.raise(300f);
    c.strongText("TFL Client", x + 9, top - c.lineHeight() - 12, t.text);
    for (int i = 0; i < primary.size(); i++) {
      final MenuAction action = primary.get(i);
      int y = top + i * row, h = row - 5;
      boolean hover = focus == i || inside(mx, my, x, y, w, h);
      this.hover[i]=Anim.approach(this.hover[i],hover?1f:0f,dt,.08f);
      SelectionStyle.panel(c,x,y,w,h,ColorMath.lerp(t.surfaceHigh,t.surfaceHover,this.hover[i]),ColorMath.lerp(t.border,t.accent,this.hover[i]));
      c.text(c.clip(action.label(), w - 18), x + 9, y + (h - c.lineHeight()) / 2, t.text, false);
      hits.add(x, y, w, h, () -> run(action));
    }
    // The close control retains its full-window position. Secondary actions wrap to the left.
    Paint.iconButton(c, width - 26, 8, 16, "close", inside(mx, my, width - 26, 8, 16, 16), false);
    hits.add(
        width - 26,
        8,
        16,
        16,
        () -> {
          click.run();
          requestClose();
        });
    int size = 25, gap = 5, columns = Math.max(1, (width / 2 - 40) / (size + gap));
    String tip = null;
    int tipX = 0, tipY = 0;
    for (int i = 0; i < secondary.size(); i++) {
      final MenuAction action = secondary.get(i);
      int sx = width - 38 - size - (i % columns) * (size + gap);
      int sy = 8 + (i / columns) * (size + gap);
      boolean hover = focus == primary.size() + i || inside(mx, my, sx, sy, size, size);
      MenuSkin.button(c, sx, sy, size, size, hover, true);
      Icons.draw(c, Icons.has(action.icon()) ? action.icon() : "gear", sx + 4, sy + 4, 2, t.text);
      hits.add(sx, sy, size, size, () -> run(action));
      if (hover) {
        tip = action.label();
        tipX = sx;
        tipY = sy + size + 5;
      }
    }
    if (tip != null) {
      String text = c.clip(tip, Math.max(20, width - 28));
      int tw = c.textWidth(text) + 14;
      int tx = Math.max(8, Math.min(tipX, width - tw - 8));
      SelectionStyle.rounded(c, tx, tipY, tw, c.lineHeight() + 12, 5, 0xF0161F2E);
      c.text(text, tx + 7, tipY + 6, t.text, false);
    }
    c.pop();
  }

  private static boolean inside(int mx, int my, int x, int y, int w, int h) {
    return mx >= x && my >= y && mx < x + w && my < y + h;
  }

  private void run(MenuAction action) {
    click.run();
    action.run();
  }

  @Override
  public boolean keyPressed(int raw, UiKey key, boolean shift) {
    if (key == UiKey.ESCAPE) {
      requestClose();
      return true;
    }
    int count = primary.size() + secondary.size();
    int step =
        key == UiKey.DOWN || key == UiKey.RIGHT
            ? 1
            : key == UiKey.UP || key == UiKey.LEFT ? -1 : key == UiKey.TAB ? (shift ? -1 : 1) : 0;
    if (step != 0 && count > 0) {
      focus = focus < 0 ? (step > 0 ? 0 : count - 1) : (focus + step + count) % count;
      return true;
    }
    if (key == UiKey.ENTER && focus >= 0) {
      run(focus < primary.size() ? primary.get(focus) : secondary.get(focus - primary.size()));
      return true;
    }
    return false;
  }

  @Override
  protected void onClosed() {
    close.run();
  }

  @Override
  public boolean pausesGame() {
    return false;
  }
}
