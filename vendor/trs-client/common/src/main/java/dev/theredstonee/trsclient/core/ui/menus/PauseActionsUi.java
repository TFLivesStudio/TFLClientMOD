package dev.theredstonee.trsclient.core.ui.menus;
import java.util.List;
import dev.theredstonee.trsclient.core.ui.menu.MenuAction;
/** Original left-hand pause rail; native chat occupies the lower right. */
public final class PauseActionsUi extends ActionListUi {
  public PauseActionsUi(String title, List<MenuAction> actions, Runnable close, Runnable click) {
    super(title, actions, close, click); navigationRail();
  }
  @Override protected boolean screenClose() { return true; }
}
