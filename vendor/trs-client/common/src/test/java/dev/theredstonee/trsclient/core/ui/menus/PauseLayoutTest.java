package dev.theredstonee.trsclient.core.ui.menus;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

class PauseLayoutTest {
  @Test
  void chatStaysInRightHalfAtSmallAndLargeGuiSizes() {
    for (int width : new int[] {214, 320, 427, 480, 854, 1280, 1920}) {
      int left = PauseLayout.chatLeft(width), size = PauseLayout.chatWidth(width);
      assertTrue(left >= width / 2);
      assertTrue(size >= 40);
      assertEquals(width - 10, left + size);
    }
  }

  @Test
  void pauseScrimIsQuarterOpacityBlack() {
    assertEquals(0, PauseLayout.SCRIM & 0xffffff);
    assertEquals(64, PauseLayout.SCRIM >>> 24);
  }
}
