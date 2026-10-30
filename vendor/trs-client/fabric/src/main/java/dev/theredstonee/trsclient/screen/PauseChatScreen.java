//? if >=26.3 {
/*package dev.theredstonee.trsclient.screen;
import dev.theredstonee.trsclient.compat.*;
import dev.theredstonee.trsclient.core.ui.menus.*;
import dev.theredstonee.trsclient.ui.*;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.input.*;
// Native chat keeps signing, commands, history, completion and clickable components.
public final class PauseChatScreen extends net.minecraft.client.gui.screens.ChatScreen implements PauseOverlay {
  public final PauseActionsUi actions;
  public PauseChatScreen(PauseActionsUi actions){super("",false,false);this.actions=actions;}
  @Override public void extractRenderState(GuiGraphicsExtractor g,int x,int y,float delta){
    super.extractRenderState(g,x,y,delta);
    Gfx gfx=Gfx.of(g);gfx.managed(()->actions.render(GfxCanvas.of(gfx,font),width,height,x,y));
  }
  @Override public void extractBackground(GuiGraphicsExtractor g,int x,int y,float delta){}
  @Override public boolean mouseClicked(MouseButtonEvent event,boolean twice){
    return actions.inputClick(event.x(),event.y(),Keys.uiButton(event.button()))||super.mouseClicked(event,twice);
  }
  @Override public boolean mouseScrolled(double x,double y,double horizontal,double vertical){
    if(x<width/2&&y<height/2)return actions.inputScroll(x,y,vertical);
    return super.mouseScrolled(x,y,horizontal,vertical);
  }
  @Override public boolean keyPressed(KeyEvent event){
    var key=Keys.ui(event.key());
    if(key==dev.theredstonee.trsclient.core.ui.UiKey.TAB&&(!input.isFocused()||event.hasControlDown())){input.setFocused(!input.isFocused());return true;}
    if(!input.isFocused()&&key!=dev.theredstonee.trsclient.core.ui.UiKey.ESCAPE)return actions.inputKey(event.key(),key,event.hasShiftDown());
    return super.keyPressed(event);
  }
  @Override public boolean charTyped(CharacterEvent event){return input.isFocused()&&super.charTyped(event);}
  @Override public boolean isPauseScreen(){return false;}
}
*///?}
