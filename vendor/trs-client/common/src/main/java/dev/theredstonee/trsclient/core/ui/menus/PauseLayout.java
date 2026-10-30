package dev.theredstonee.trsclient.core.ui.menus;

/** Shared pause composition, independent of saved HUD/chat preferences. */
public final class PauseLayout {
  public static final int SCRIM = 0x40000000;

  private PauseLayout() {}

  public static int chatWidth(int width) {
    return Math.min(320, Math.max(40, width / 2 - 16));
  }

  public static int chatLeft(int width) {
    return width - 10 - chatWidth(width);
  }
}
