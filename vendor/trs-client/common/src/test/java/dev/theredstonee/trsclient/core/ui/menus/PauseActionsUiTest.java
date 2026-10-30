package dev.theredstonee.trsclient.core.ui.menus;

import static org.junit.jupiter.api.Assertions.*;

import dev.theredstonee.trsclient.core.ui.*;
import dev.theredstonee.trsclient.core.ui.menu.MenuAction;
import java.util.*;
import org.junit.jupiter.api.Test;

class PauseActionsUiTest {
  static final class Draw implements Canvas {
    final List<String> text = new ArrayList<>();
    int buttons;

    public boolean rounded(int x, int y, int w, int h, int r, int color) {
      if (w > 80 && h >= 12) buttons++;
      return true;
    }

    public void fill(int a, int b, int c, int d, int color) {}

    public void text(String s, int x, int y, int color, boolean shadow) {
      text.add(s);
    }

    public int textWidth(String s) {
      return s.length() * 5;
    }

    public int lineHeight() {
      return 9;
    }

    public String clip(String s, int max) {
      return s.substring(0, Math.min(s.length(), Math.max(0, max / 5)));
    }

    public void flush() {}

    public void scissor(int a, int b, int c, int d) {}

    public void noScissor() {}

    public void raise(float z) {}

    public void push() {}

    public void pop() {}

    public void translate(float x, float y) {}

    public void scale(float f) {}
  }

  @Test
  void allSixPrimaryButtonsRemainVisibleAndClickableAtSmallAndLargeGuiSizes() {
    for (int[] size : new int[][] {{320, 240}, {427, 240}, {480, 270}, {960, 540}}) {
      int[] called = {-1};
      List<MenuAction> main = new ArrayList<>();
      for (int i = 0; i < 6; i++) {
        final int index = i;
        main.add(new MenuAction("Primary " + i, "gear", () -> called[0] = index));
      }
      PauseActionsUi ui =
          new PauseActionsUi("Pause", main, Collections.emptyList(), () -> {}, () -> {});
      Draw draw = new Draw();
      ui.render(draw, size[0], size[1], -1, -1);
      assertTrue(draw.buttons >= 6, "Each primary action has a rounded background");
      int x = Math.max(12, Math.min(40, size[0] / 16));
      int row = Math.max(18, Math.min(35, (size[1] - 54) / 6));
      int top = Math.max(30, (size[1] - 6 * row) / 2);
      for (int i = 0; i < 6; i++) {
        assertTrue(top + (i + 1) * row <= size[1]);
        assertTrue(draw.text.contains("Primary " + i));
        assertTrue(ui.inputClick(x + 20, top + i * row + 8, 0));
        assertEquals(i, called[0]);
      }
    }
  }

  @Test
  void secondaryActionUsesCornerIconWithTooltipAndRemainsKeyboardAccessible() {
    int[] called = {0};
    List<MenuAction> main = Arrays.asList(new MenuAction("Resume", "play", () -> {}));
    PauseActionsUi ui =
        new PauseActionsUi(
            "Pause",
            main,
            Arrays.asList(new MenuAction("Statistics", "gauge", () -> called[0]++)),
            () -> {},
            () -> {});
    Draw draw = new Draw();
    ui.render(draw, 480, 270, 422, 15);
    assertTrue(draw.text.contains("Statistics"));
    assertTrue(ui.inputClick(422, 15, 0));
    assertEquals(1, called[0]);
    ui.keyPressed(0, UiKey.TAB, false);
    ui.keyPressed(0, UiKey.TAB, false);
    ui.keyPressed(0, UiKey.ENTER, false);
    assertEquals(2, called[0]);
    draw.text.clear();
    ui.keyPressed(0, UiKey.TAB, false);
    ui.render(draw, 480, 270, -1, -1);
    assertFalse(draw.text.contains("Statistics"));
  }
}
