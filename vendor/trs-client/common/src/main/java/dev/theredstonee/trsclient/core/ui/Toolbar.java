package dev.theredstonee.trsclient.core.ui;

/** Wrapping native action strip. Shared drawing and hit geometry; never touches renderer state. */
public final class Toolbar {
  private final Canvas canvas;
  private final Hits hits;
  private final int left, width, mx, my;
  private int x, y;

  public Toolbar(Canvas canvas, Hits hits, int left, int top, int width, int mx, int my) {
    this.canvas = canvas;
    this.hits = hits;
    this.left = left;
    this.width = Math.max(24, width);
    this.mx = mx;
    this.my = my;
    x = left;
    y = top;
  }

  public void action(String label, Runnable action) {
    int w = Math.min(width, Math.max(26, canvas.textWidth(label) + 18));
    if (x > left && x + w > left + width) {
      x = left;
      y += 28;
    }
    boolean hover = mx >= x && mx < x + w && my >= y && my < y + 24;
    Paint.button(canvas, x, y, w, 24, label, false, hover);
    hits.add(x, y, w, 24, action);
    x += w + 4;
  }

  public int bottom() {
    return y + 24;
  }
}
