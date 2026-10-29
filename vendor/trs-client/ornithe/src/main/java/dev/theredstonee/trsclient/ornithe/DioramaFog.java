package dev.theredstonee.trsclient.ornithe;
import org.lwjgl.opengl.*;
import dev.theredstonee.trsclient.core.ui.TextureRef;
/** Original Diorama eight-layer fog shader, with a compatibility-profile vertex stage. */
final class DioramaFog implements AutoCloseable {
 private final OrnitheClient client;private int program;private TextureRef noise,mask;private String error="";
 DioramaFog(OrnitheClient c){client=c;}
 private int shader(int type,String source){int id=GL20.glCreateShader(type);GL20.glShaderSource(id,source);GL20.glCompileShader(id);if(GL20.glGetShaderi(id,GL20.GL_COMPILE_STATUS)==0){String reason=GL20.glGetShaderInfoLog(id,8192);GL20.glDeleteShader(id);throw new IllegalStateException(reason);}return id;}
 private boolean prepare(){if(!error.isEmpty())return false;if(program!=0)return true;int vertex=0,fragment=0;
  try(var source=DioramaFog.class.getResourceAsStream("/assets/tflcore/shaders/diorama-fog.frag")){
   vertex=shader(GL20.GL_VERTEX_SHADER,"#version 330 compatibility\nout vec2 maskUv;void main(){gl_Position=gl_ModelViewProjectionMatrix*gl_Vertex;maskUv=gl_MultiTexCoord0.xy;}");
   fragment=shader(GL20.GL_FRAGMENT_SHADER,new String(source.readAllBytes(),java.nio.charset.StandardCharsets.UTF_8));program=GL20.glCreateProgram();GL20.glAttachShader(program,vertex);GL20.glAttachShader(program,fragment);GL20.glLinkProgram(program);
   if(GL20.glGetProgrami(program,GL20.GL_LINK_STATUS)==0)throw new IllegalStateException(GL20.glGetProgramInfoLog(program,8192));
   int[] pixels=dev.diorama.render.FogNoise.createNoise();for(int i=0;i<pixels.length;i++){int p=pixels[i];pixels[i]=p&0xff00ff00|(p&255)<<16|(p>>>16&255);}noise=client.canvas.upload("diorama/fog-noise",256,256,pixels);return true;
  }catch(Exception|LinkageError e){error=e.toString();OrnitheClient.LOG.warn("Diorama fog shader unavailable: {}",error);if(program!=0)GL20.glDeleteProgram(program);program=0;return false;}
  finally{if(vertex!=0)GL20.glDeleteShader(vertex);if(fragment!=0)GL20.glDeleteShader(fragment);}
 }
 boolean draw(int x,int y,int w,int h,int[] coverage,double u0,double v0,double du,double dv,int scaleCode,double maskU,double maskV,boolean cave){
  if(!prepare())return false;
  if(coverage!=null)mask=client.canvas.upload("diorama/fog-mask",256,256,coverage);if(mask==null)return false;
  int oldProgram=GL11.glGetInteger(GL20.GL_CURRENT_PROGRAM),active=GL11.glGetInteger(GL13.GL_ACTIVE_TEXTURE);
  net.minecraft.client.render.platform.GlStateManager.activeTexture(GL13.GL_TEXTURE0);int texture0=GL11.glGetInteger(GL11.GL_TEXTURE_BINDING_2D);net.minecraft.client.render.platform.GlStateManager.activeTexture(GL13.GL_TEXTURE1);int texture1=GL11.glGetInteger(GL11.GL_TEXTURE_BINDING_2D);
  try{
   client.mc.getTextureManager().bind((net.minecraft.resource.Identifier)noise.id);GL11.glTexParameteri(GL11.GL_TEXTURE_2D,GL11.GL_TEXTURE_WRAP_S,GL11.GL_REPEAT);GL11.glTexParameteri(GL11.GL_TEXTURE_2D,GL11.GL_TEXTURE_WRAP_T,GL11.GL_REPEAT);
   net.minecraft.client.render.platform.GlStateManager.activeTexture(GL13.GL_TEXTURE0);client.mc.getTextureManager().bind((net.minecraft.resource.Identifier)mask.id);GL11.glTexParameteri(GL11.GL_TEXTURE_2D,GL11.GL_TEXTURE_WRAP_S,GL12.GL_CLAMP_TO_EDGE);GL11.glTexParameteri(GL11.GL_TEXTURE_2D,GL11.GL_TEXTURE_WRAP_T,GL12.GL_CLAMP_TO_EDGE);
   GL20.glUseProgram(program);GL20.glUniform1i(GL20.glGetUniformLocation(program,"Sampler0"),0);GL20.glUniform1i(GL20.glGetUniformLocation(program,"Sampler1"),1);
   GL20.glUniform1f(GL20.glGetUniformLocation(program,"GameTime"),client.modules.worldMapCloudMotion.get()?(System.nanoTime()%1_200_000_000_000L)/1_200_000_000_000f:0);
   GL20.glUniform4f(GL20.glGetUniformLocation(program,"ColorModulator"),1,1,1,1);
   int nu=Math.floorMod((int)Math.floor(maskU/128),8),nv=Math.floorMod((int)Math.floor(maskV/128),8);
   GL20.glUniform4f(GL20.glGetUniformLocation(program,"params"),nu*32/255f,nv*32/255f,cave?128/255f:0,(scaleCode*16+(client.modules.worldMapCloudQuality.get()==dev.theredstonee.trsclient.core.map.volume.CloudQuality.LOW?4:0))/255f);
   GL11.glBegin(GL11.GL_QUADS);GL11.glTexCoord2d(u0,v0);GL11.glVertex2i(x,y);GL11.glTexCoord2d(u0+du,v0);GL11.glVertex2i(x+w,y);GL11.glTexCoord2d(u0+du,v0+dv);GL11.glVertex2i(x+w,y+h);GL11.glTexCoord2d(u0,v0+dv);GL11.glVertex2i(x,y+h);GL11.glEnd();return true;
  }finally{GL20.glUseProgram(oldProgram);net.minecraft.client.render.platform.GlStateManager.activeTexture(GL13.GL_TEXTURE1);net.minecraft.client.render.platform.GlStateManager.bindTexture(texture1);net.minecraft.client.render.platform.GlStateManager.activeTexture(GL13.GL_TEXTURE0);net.minecraft.client.render.platform.GlStateManager.bindTexture(texture0);net.minecraft.client.render.platform.GlStateManager.activeTexture(active);}
 }
 public boolean healthy(){return program!=0&&error.isEmpty();}
 public void close(){if(program!=0)GL20.glDeleteProgram(program);program=0;if(noise!=null)client.canvas.release(noise);if(mask!=null)client.canvas.release(mask);noise=mask=null;}
}
