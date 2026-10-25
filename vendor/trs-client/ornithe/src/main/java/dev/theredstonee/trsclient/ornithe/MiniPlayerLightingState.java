package dev.theredstonee.trsclient.ornithe;
import java.nio.FloatBuffer;
import net.minecraft.client.render.platform.GlStateManager;
import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL11;
/** Restore fixed-function light parameters in eye space as well as their enable flags. */
final class MiniPlayerLightingState {
 private static final java.lang.invoke.MethodHandle GET_LIGHT=findGetLight();
 private static java.lang.invoke.MethodHandle findGetLight(){
  var type=java.lang.invoke.MethodType.methodType(void.class,int.class,int.class,FloatBuffer.class);
  for(String name:new String[]{"glGetLight","glGetLightfv"})try{return java.lang.invoke.MethodHandles.publicLookup().findStatic(GL11.class,name,type);}catch(NoSuchMethodException|IllegalAccessException missing){}
  throw new IllegalStateException("Native light-state query unavailable");
 }
 private static void getLight(int light,int parameter,FloatBuffer buffer){try{GET_LIGHT.invokeExact(light,parameter,buffer);}catch(Throwable failure){throw new IllegalStateException("Native light-state query",failure);}}
 private static final int[] PARAMS={GL11.GL_POSITION,GL11.GL_DIFFUSE,GL11.GL_AMBIENT,GL11.GL_SPECULAR};
 private final FloatBuffer[][] lights=new FloatBuffer[2][4];
 private final FloatBuffer ambient=BufferUtils.createFloatBuffer(16);
 private int shade,materialFace,materialMode,alphaFunc,depthFunc;private float alphaRef;
 MiniPlayerLightingState(){for(int l=0;l<2;l++)for(int p=0;p<4;p++)lights[l][p]=BufferUtils.createFloatBuffer(16);}
 void capture(){
  shade=GL11.glGetInteger(GL11.GL_SHADE_MODEL);materialFace=GL11.glGetInteger(GL11.GL_COLOR_MATERIAL_FACE);materialMode=GL11.glGetInteger(GL11.GL_COLOR_MATERIAL_PARAMETER);
  alphaFunc=GL11.glGetInteger(GL11.GL_ALPHA_TEST_FUNC);alphaRef=GL11.glGetFloat(GL11.GL_ALPHA_TEST_REF);depthFunc=GL11.glGetInteger(GL11.GL_DEPTH_FUNC);
  ambient.clear();GL11.glGetFloat(GL11.GL_LIGHT_MODEL_AMBIENT,ambient);
  for(int l=0;l<2;l++)for(int p=0;p<4;p++){lights[l][p].clear();getLight(GL11.GL_LIGHT0+l,PARAMS[p],lights[l][p]);}
 }
 void restore(){
  int mode=GL11.glGetInteger(GL11.GL_MATRIX_MODE);GlStateManager.matrixMode(GL11.GL_MODELVIEW);GlStateManager.pushMatrix();GlStateManager.loadIdentity();
  try{for(int l=0;l<2;l++)for(int p=0;p<4;p++)GL11.glLight(GL11.GL_LIGHT0+l,PARAMS[p],lights[l][p]);}
  finally{GlStateManager.popMatrix();GlStateManager.matrixMode(mode);}
  GL11.glLightModel(GL11.GL_LIGHT_MODEL_AMBIENT,ambient);GlStateManager.shadeModel(shade);GlStateManager.colorMaterial(materialFace,materialMode);GlStateManager.alphaFunc(alphaFunc,alphaRef);GlStateManager.depthFunc(depthFunc);
 }
}
