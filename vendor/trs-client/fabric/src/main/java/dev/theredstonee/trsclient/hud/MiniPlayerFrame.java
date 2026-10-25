//? if >=26.3 {
/*package dev.theredstonee.trsclient.hud;
import net.minecraft.client.renderer.state.gui.pip.*;
import net.minecraft.client.gui.navigation.ScreenRectangle;
// Owned player render state, never a second entity. Coordinates are absolute GUI pixels.
public record MiniPlayerFrame(GuiEntityRenderState entity, long serial, float opacity) implements PictureInPictureRenderState {
 public int x0(){return entity.x0();} public int y0(){return entity.y0();}
 public int x1(){return entity.x1();} public int y1(){return entity.y1();}
 public float scale(){return entity.scale();}
 public ScreenRectangle scissorArea(){return entity.scissorArea();}
 public ScreenRectangle bounds(){return entity.bounds();}
}
*///?}
