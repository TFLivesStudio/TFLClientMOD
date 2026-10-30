package dev.theredstonee.trsclient.core.ui.menus;
import java.util.List;
import dev.theredstonee.trsclient.core.ui.menu.MenuAction;
/** Transparent actions above the native chat history. */
public final class PauseActionsUi extends ActionListUi {
  public PauseActionsUi(String title, List<MenuAction> actions, Runnable close, Runnable click) {
    super(title, actions, close, click); navigationRail();
  }
  @Override protected boolean topRail() { return true; }
  @Override protected int railHeight(int height) { return Math.max(50, height / 2); }
}
