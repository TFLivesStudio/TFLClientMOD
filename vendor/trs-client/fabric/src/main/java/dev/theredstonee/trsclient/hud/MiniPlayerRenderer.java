//? if >=26.3 {
/*package dev.theredstonee.trsclient.hud;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.renderpearl.api.textures.FilterMode;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.render.TextureSetup;
import net.minecraft.client.gui.render.pip.*;
import net.minecraft.client.renderer.*;
import net.minecraft.client.renderer.state.gui.*;
import net.minecraft.client.renderer.state.gui.pip.GuiEntityRenderState;
import dev.theredstonee.trsclient.mixin.MiniPlayerTextureAccessor;
// The engine owns render passes, depth, viewports and attachment lifetime. Two textures total.
public final class MiniPlayerRenderer extends PictureInPictureRenderer<MiniPlayerFrame> {
 private static MiniPlayerRenderer active;
 private final NativeEntity nativeEntity=new NativeEntity();
 private long serial=Long.MIN_VALUE;
 public long renders,blits;
 public static long rendered(){return active==null?0:active.renders;}
 public static long composited(){return active==null?0:active.blits;}
 public MiniPlayerRenderer(){active=this;}
 private static final class NativeEntity extends GuiEntityRenderer {
  NativeEntity(){super(Minecraft.getInstance().getEntityRenderDispatcher());}
  void submit(GuiEntityRenderState s,PoseStack pose,SubmitNodeCollector collector){super.renderToTexture(s,pose,collector);}
 }
 public Class<MiniPlayerFrame> getRenderStateClass(){return MiniPlayerFrame.class;}
 protected String getTextureLabel(){return "TFL Mini Player";}
 protected float getTranslateY(int h,int guiScale){return h/2f;}
 protected boolean textureIsReadyToBlit(MiniPlayerFrame s){return serial==s.serial();}
 protected void renderToTexture(MiniPlayerFrame s,PoseStack p,SubmitNodeCollector c){nativeEntity.submit(s.entity(),p,c);serial=s.serial();renders++;}
 protected void blitTexture(MiniPlayerFrame s,GuiRenderState g){
  int a=Math.max(0,Math.min(255,Math.round(s.opacity()*255))),color=a<<24|a<<16|a<<8|a;
  g.addBlitToCurrentLayer(new BlitRenderState(RenderPipelines.GUI_TEXTURED_PREMULTIPLIED_ALPHA,
   TextureSetup.singleTexture(((MiniPlayerTextureAccessor)(Object)this).tfl$miniTexture(),RenderSystem.getSamplerCache().getRepeat(FilterMode.LINEAR)),
   s.pose(),s.x0(),s.y0(),s.x1(),s.y1(),0,1,1,0,color,s.scissorArea(),null));blits++;
 }
 public static void releaseCached(){if(active!=null){active.releaseAttachments();active.serial=Long.MIN_VALUE;}}
 private void releaseAttachments(){
  MiniPlayerTextureAccessor a=(MiniPlayerTextureAccessor)(Object)this;
  if(a.tfl$miniTexture()!=null)a.tfl$miniTexture().close();
  if(a.tfl$miniDepthView()!=null)a.tfl$miniDepthView().close();
  if(a.tfl$miniColor()!=null)a.tfl$miniColor().close();
  if(a.tfl$miniDepth()!=null)a.tfl$miniDepth().close();
  a.tfl$miniTexture(null);a.tfl$miniDepthView(null);a.tfl$miniColor(null);a.tfl$miniDepth(null);
 }
 public void close(){super.close();nativeEntity.close();serial=Long.MIN_VALUE;}
}
*///?}
