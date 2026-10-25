package dev.theredstonee.trsclient.ornithe.dev;
import dev.theredstonee.trsclient.ornithe.OrnitheClient;
import org.lwjgl.opengl.GL11;
import java.util.*;
/** Native state regression around the complete HUD scope, including the owned framebuffer. */
public final class MiniGlProbe {
 private static Map<Integer,float[]> snapshot(){
  Map<Integer,float[]> values=new LinkedHashMap<>();
  int[] vector={GL11.GL_CURRENT_COLOR,GL11.GL_COLOR_CLEAR_VALUE,GL11.GL_VIEWPORT,GL11.GL_SCISSOR_BOX,GL11.GL_MODELVIEW_MATRIX,GL11.GL_PROJECTION_MATRIX,GL11.GL_TEXTURE_MATRIX};
  for(int name:vector){var b=org.lwjgl.BufferUtils.createFloatBuffer(16);GL11.glGetFloat(name,b);float[] copy=new float[16];for(int i=0;i<16;i++)copy[i]=b.get(i);values.put(name,copy);}
  int[] integers={0x8CA6,0x8CA7,GL11.GL_MATRIX_MODE,0x84E0,GL11.GL_TEXTURE_BINDING_2D,0x80C9,0x80C8,0x80CB,0x80CA,GL11.GL_ALPHA_TEST_FUNC,GL11.GL_DEPTH_FUNC,GL11.GL_SHADE_MODEL,GL11.GL_COLOR_MATERIAL_FACE,GL11.GL_COLOR_MATERIAL_PARAMETER,0x8894,0x8895};
  for(int name:integers)values.put(name,new float[]{GL11.glGetInteger(name)});
  int[] caps={GL11.GL_BLEND,GL11.GL_TEXTURE_2D,GL11.GL_DEPTH_TEST,GL11.GL_CULL_FACE,GL11.GL_SCISSOR_TEST,GL11.GL_ALPHA_TEST,GL11.GL_LIGHTING,GL11.GL_LIGHT0,GL11.GL_LIGHT1,GL11.GL_COLOR_MATERIAL,GL11.GL_NORMALIZE,0x803A};
  for(int name:caps)values.put(name,new float[]{GL11.glIsEnabled(name)?1:0});
  return values;
 }
 public static void verify(OrnitheClient c){
  Map<Integer,float[]> before=snapshot();float yaw=c.mc.player.yaw,body=c.mc.player.bodyYaw,head=c.mc.player.headYaw;Object camera=c.mc.getCamera();int perspective=c.mc.options.perspective;
  var w=c.window();c.canvas.begin(w.getWidth(),w.getHeight(),w.getScale());try{c.miniPlayer.state.invalidate();c.miniPlayer.draw(c.canvas);}finally{c.canvas.end();}
  Map<Integer,float[]> after=snapshot();
  for(var e:before.entrySet()){float[] a=e.getValue(),b=after.get(e.getKey());for(int i=0;i<a.length;i++)if(Math.abs(a[i]-b[i])>1e-5)throw new AssertionError("Mini Player GL state 0x"+Integer.toHexString(e.getKey())+"["+i+"]: "+a[i]+" -> "+b[i]);}
  if(c.mc.player.yaw!=yaw||c.mc.player.bodyYaw!=body||c.mc.player.headYaw!=head||c.mc.getCamera()!=camera||c.mc.options.perspective!=perspective)throw new AssertionError("Mini Player changed entity/world camera state");
 }
}
