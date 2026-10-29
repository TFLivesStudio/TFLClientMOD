package dev.theredstonee.trsclient.ornithe;

import dev.theredstonee.trsclient.core.ui.*;
import net.minecraft.client.gui.screen.Screen;
import org.lwjgl.input.Keyboard;
import org.lwjgl.input.Mouse;

/** Only translates native input/lifecycle. All control behavior remains in the common UI. */
public final class CoreScreen extends Screen {
  public final UiScreen ui;
  public final boolean home;

  public CoreScreen(UiScreen ui, boolean home) {
    this.ui = ui;
    this.home = home;
  }

  @Override
  public void render(int mx, int my, float delta) {
    OrnitheClient c = OrnitheClient.get();
    c.canvas.begin(width, height, c.window().getScale());
    try {
      ui.render(c.canvas, width, height, mx, my);
      if (c.navigation != null)
        c.navigation.screenshots.render(c.canvas, width, height, mx, my, true);
    } finally {
      c.canvas.end();
    }
  }

  @Override
  protected void mouseClicked(int x, int y, int b) {
    if (OrnitheClient.get().navigation != null
        && OrnitheClient.get().navigation.screenshots.mouseClicked(x, y, b)) return;
    ui.inputClick(x, y, b);
  }

  @Override
  protected void mouseReleased(int x, int y, int b) {
    ui.inputRelease(x, y, b);
  }

  @Override
  protected void mouseDragged(int x, int y, int b, long elapsed) {
    ui.inputDrag(x, y, b);
  }

  @Override
  public void handleMouse() {
    super.handleMouse();
    int wheel = Mouse.getEventDWheel();
    if (wheel != 0)
      ui.inputScroll(
          Mouse.getEventX() * width / minecraft.width,
          height - Mouse.getEventY() * height / minecraft.height - 1,
          Math.signum(wheel));
  }

  @Override
  protected void keyPressed(char typed, int raw) {
    UiKey key = logical(raw);
    if (isControlDown() && raw == Keyboard.KEY_V) {
      String clipboard = Screen.getClipboard();
      if (clipboard != null) for (int i = 0; i < clipboard.length(); i++) ui.inputChar(clipboard.charAt(i));
      return;
    }
    if (isControlDown() && raw == Keyboard.KEY_A) key = UiKey.SELECT_ALL;
    if (!ui.inputKey(raw, key, isShiftDown()) && key == UiKey.NONE && TextInput.allowed(typed))
      ui.inputChar(typed);
  }

  @Override
  public boolean shouldPauseGame() {
    return ui.pausesGame() && !home;
  }

  @Override
  public void removed() {
    ui.removed();
    if(home)dev.theredstonee.trsclient.core.ui.background.MenuBackground.shared().close();
    OrnitheClient c = OrnitheClient.get();
    if (c != null) c.save();
  }

  public static UiKey logical(int key) {
    return switch (key) {
      case 1 -> UiKey.ESCAPE;
      case 28, 156 -> UiKey.ENTER;
      case 14 -> UiKey.BACKSPACE;
      case 211 -> UiKey.DELETE;
      case 15 -> UiKey.TAB;
      case 203 -> UiKey.LEFT;
      case 205 -> UiKey.RIGHT;
      case 200 -> UiKey.UP;
      case 208 -> UiKey.DOWN;
      case 199 -> UiKey.HOME;
      case 207 -> UiKey.END;
      default -> UiKey.NONE;
    };
  }
}
