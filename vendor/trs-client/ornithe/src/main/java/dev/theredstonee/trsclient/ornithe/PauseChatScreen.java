package dev.theredstonee.trsclient.ornithe;
import dev.theredstonee.trsclient.core.ui.menus.*;
import net.minecraft.client.gui.screen.game.ChatScreen;
/** Uses the game's input, history, completion and clickable chat implementation. */
public final class PauseChatScreen extends ChatScreen implements PauseOverlay {
  public final PauseActionsUi actions;
  public PauseChatScreen(PauseActionsUi actions) { super(""); this.actions=actions; }
  @Override public void render(int x,int y,float delta) {
    super.render(x,y,delta);
    var c=OrnitheClient.get();c.canvas.begin(width,height,c.window().getScale());
    try { actions.render(c.canvas,width,height,x,y); } finally {c.canvas.end();}
  }
  @Override protected void mouseClicked(int x,int y,int button) {
    if(actions.inputClick(x,y,button))return;
    super.mouseClicked(x,y,button);
  }
  @Override protected void keyPressed(char ch,int key) {
    if(key==15){chatField.setFocused(!chatField.isFocused());return;}
    if(!chatField.isFocused()&&key!=1){actions.inputKey(key,CoreScreen.logical(key),isShiftDown());return;}
    super.keyPressed(ch,key);
    if((key==28||key==156)&&minecraft.screen==null)minecraft.openScreen(this);
  }
  @Override public boolean shouldPauseGame(){return false;}
}
