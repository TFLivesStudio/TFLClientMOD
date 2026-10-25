package dev.theredstonee.trsclient.ornithe;

import dev.theredstonee.trsclient.core.module.HudModule;
import dev.theredstonee.trsclient.core.ui.Canvas;
import dev.theredstonee.trsclient.core.ui.menu.HudItem;
import net.minecraft.client.options.GameOptions;
import net.minecraft.client.options.KeyBinding;

/** Same layout as the legacy widget, with labels following actual vanilla bindings. */
public final class OrnitheKeystrokes implements HudItem {
  private final OrnitheClient c;
  private final int[] codes = {
    Integer.MIN_VALUE, Integer.MIN_VALUE, Integer.MIN_VALUE, Integer.MIN_VALUE
  };
  private final String[] labels = new String[4];
  private static final String[] CPS = new String[100];

  static {
    for (int i = 0; i < CPS.length; i++) CPS[i] = i + " CPS";
  }

  public OrnitheKeystrokes(OrnitheClient c) {
    this.c = c;
  }

  public HudModule module() {
    return c.modules.keystrokes;
  }

  public int width() {
    return 70;
  }

  public int height() {
    return 48
        + (c.modules.keystrokesShowCps.get() ? 28 : 22)
        + (c.modules.keystrokesShowSpace.get() ? 13 : 0);
  }

  private String label(int i, KeyBinding key) {
    if (codes[i] != key.getKeyCode()) {
      codes[i] = key.getKeyCode();
      labels[i] = c.canvas.clip(GameOptions.getKeyName(codes[i]), 18);
    }
    return labels[i];
  }

  public void draw(Canvas canvas) {
    var o = c.mc.options;
    key(canvas, o.forwardKey, label(0, o.forwardKey), 24, 0, 22, 22);
    key(canvas, o.leftKey, label(1, o.leftKey), 0, 24, 22, 22);
    key(canvas, o.backKey, label(2, o.backKey), 24, 24, 22, 22);
    key(canvas, o.rightKey, label(3, o.rightKey), 48, 24, 22, 22);
    int mh = c.modules.keystrokesShowCps.get() ? 28 : 22;
    key(canvas, o.attackKey, "LMB", 0, 48, 34, mh);
    key(canvas, o.useKey, "RMB", 36, 48, 34, mh);
    if (c.modules.keystrokesShowCps.get()) {
      long now = System.nanoTime() / 1000000;
      center(canvas, CPS[Math.min(99, c.left.count(now))], 0, 64, 34, o.attackKey.isPressed());
      center(canvas, CPS[Math.min(99, c.right.count(now))], 36, 64, 34, o.useKey.isPressed());
    }
    if (c.modules.keystrokesShowSpace.get()) {
      int y = 50 + mh;
      canvas.fill(0, y, 70, y + 11, o.jumpKey.isPressed() ? c.modules.keystrokesPressedColor.argb() : module().backgroundArgb());
      canvas.fill(21, y + 5, 49, y + 6, o.jumpKey.isPressed() ? c.modules.keystrokesPressedText.argb() : module().textColor.argb());
    }
  }

  private void key(Canvas canvas, KeyBinding key, String label, int x, int y, int w, int h) {
    canvas.fill(x, y, x + w, y + h, key.isPressed() ? c.modules.keystrokesPressedColor.argb() : module().backgroundArgb());
    canvas.text(label, x + (w - canvas.textWidth(label)) / 2, y + 5, key.isPressed() ? c.modules.keystrokesPressedText.argb() : module().textColor.argb(), module().shadow() && !key.isPressed());
  }

  private void center(Canvas canvas, String text, int x, int y, int w, boolean pressed) {
    canvas.text(
        text,
        x + (w - canvas.textWidth(text)) / 2,
        y,
        pressed ? c.modules.keystrokesPressedText.argb() : module().textColor.argb(),
        module().shadow() && !pressed);
  }
}
