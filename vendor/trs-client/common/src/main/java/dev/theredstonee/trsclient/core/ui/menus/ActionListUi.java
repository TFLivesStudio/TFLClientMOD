package dev.theredstonee.trsclient.core.ui.menus;

import dev.theredstonee.trsclient.core.ui.*;
import dev.theredstonee.trsclient.core.ui.menu.MenuAction;
import java.util.List;

/**
 * Shared action window for platform navigation and diagnostics; all controls use the common design
 * system.
 */
public class ActionListUi extends WindowUi {
  private final String label;
  private String description = "";
  private boolean scrim=true;
  private boolean navigationRail;
  public ActionListUi navigationRail(){navigationRail=true;scrim=false;return this;}
  protected boolean bareRail(){return navigationRail;}
  public ActionListUi transparent(){scrim=false;return this;}
  protected boolean screenScrim(){return scrim;}
  private final List<MenuAction> actions;
  private final Runnable close, click;
  private int scroll, maxScroll, focus=-1,rowHeight=25;

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
    int row = bareRail() ? Math.max(16,Math.min(29,h/Math.max(1,actions.size()))) : 25;
    rowHeight=row;maxScroll = Math.max(0, actions.size() * row - h);
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
      boolean hover=focus==i||(mx>=x && mx<x+w && my>=top && my<top+row-3);
      if (bareRail()) {
        if (hover) {
          SelectionStyle.rounded(c,x,top,w-4,row-3,4,0x58344B69);
          c.fill(x,top+3,x+2,top+row-6,Theme.get().accent);
        }
      } else MenuSkin.button(c,x,top,w-4,22,hover,true);
      c.text(c.clip(action.label(),w-18),x+9,top+(row-3-c.lineHeight())/2,
          bareRail()?0xFFF4F7FC:hover?Theme.get().text:Theme.get().textDim,true);
      hits.add(
          x,
          top,
          w - 4,
          row-3,
          () -> {
            playClick();
            action.run();
          });
    }
    hits.noClip();
    c.noScissor();
  }

  @Override public boolean keyPressed(int raw,UiKey key,boolean shift){
    if(actions.isEmpty())return super.keyPressed(raw,key,shift);
    int step=key==UiKey.DOWN||key==UiKey.RIGHT?1:key==UiKey.UP||key==UiKey.LEFT?-1:key==UiKey.TAB?(shift?-1:1):0;
    if(step!=0){focus=focus<0?(step>0?0:actions.size()-1):(focus+step+actions.size())%actions.size();scroll=Math.max(0,Math.min(maxScroll,focus*rowHeight));return true;}
    if(key==UiKey.ENTER&&focus>=0){playClick();actions.get(focus).run();return true;}
    return super.keyPressed(raw,key,shift);
  }
  public boolean mouseScrolled(double x, double y, double delta) {
    scroll = Math.max(0, Math.min(maxScroll, scroll - (int) (delta * 25)));
    return true;
  }

  protected void onClosed() {
    close.run();
  }
}
