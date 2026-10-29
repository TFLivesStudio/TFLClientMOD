package dev.theredstonee.trsclient.ornithe;
import dev.theredstonee.trsclient.core.ui.*;
import org.lwjgl.opengl.*;
/** Supersampled owned map target; the final shader clips and antialiases the circular edge. */
final class DioramaMinimapTarget implements AutoCloseable {
 private int fbo,texture,edge,program;private long updated;private boolean failed;
 void draw(OrnitheClient client,Canvas canvas,int size,boolean round,int opacity,Runnable render){
  if(failed||!GLContext.getCapabilities().OpenGL30){render.run();return;}
  int pixels=Math.min(512,size*2),oldFramebuffer=GL11.glGetInteger(GL30.GL_FRAMEBUFFER_BINDING);
  int oldProgram=GL11.glGetInteger(GL20.GL_CURRENT_PROGRAM);
  GL11.glPushAttrib(GL11.GL_ALL_ATTRIB_BITS);
  try{
   if(edge!=pixels){releaseTarget();edge=pixels;fbo=GL30.glGenFramebuffers();texture=GL11.glGenTextures();GL30.glBindFramebuffer(GL30.GL_FRAMEBUFFER,fbo);net.minecraft.client.render.platform.GlStateManager.bindTexture(texture);GL11.glTexImage2D(GL11.GL_TEXTURE_2D,0,GL11.GL_RGBA8,edge,edge,0,GL11.GL_RGBA,GL11.GL_UNSIGNED_BYTE,(java.nio.ByteBuffer)null);GL11.glTexParameteri(GL11.GL_TEXTURE_2D,GL11.GL_TEXTURE_MIN_FILTER,GL11.GL_LINEAR);GL11.glTexParameteri(GL11.GL_TEXTURE_2D,GL11.GL_TEXTURE_MAG_FILTER,GL11.GL_LINEAR);GL30.glFramebufferTexture2D(GL30.GL_FRAMEBUFFER,GL30.GL_COLOR_ATTACHMENT0,GL11.GL_TEXTURE_2D,texture,0);if(GL30.glCheckFramebufferStatus(GL30.GL_FRAMEBUFFER)!=GL30.GL_FRAMEBUFFER_COMPLETE)throw new IllegalStateException("Minimap framebuffer incomplete");}
   if(System.nanoTime()-updated>1_000_000_000L/60){
    GL30.glBindFramebuffer(GL30.GL_FRAMEBUFFER,fbo);GL11.glViewport(0,0,edge,edge);GL11.glDisable(GL11.GL_SCISSOR_TEST);GL11.glDisable(GL11.GL_DEPTH_TEST);GL11.glClearColor(0,0,0,0);GL11.glClear(GL11.GL_COLOR_BUFFER_BIT);
    GL11.glMatrixMode(GL11.GL_PROJECTION);GL11.glPushMatrix();GL11.glLoadIdentity();GL11.glOrtho(0,size,size,0,-1000,1000);GL11.glMatrixMode(GL11.GL_MODELVIEW);GL11.glPushMatrix();GL11.glLoadIdentity();
    try{client.canvas.withSurface(size,size,edge/size,render);updated=System.nanoTime();}finally{GL11.glPopMatrix();GL11.glMatrixMode(GL11.GL_PROJECTION);GL11.glPopMatrix();GL11.glMatrixMode(GL11.GL_MODELVIEW);}
   }
  }catch(RuntimeException|LinkageError error){failed=true;OrnitheClient.LOG.warn("Native minimap target unavailable",error);}
  finally{GL30.glBindFramebuffer(GL30.GL_FRAMEBUFFER,oldFramebuffer);GL11.glPopAttrib();int bound=GL11.glGetInteger(GL11.GL_TEXTURE_BINDING_2D);net.minecraft.client.render.platform.GlStateManager.bindTexture(0);net.minecraft.client.render.platform.GlStateManager.bindTexture(bound);}
  if(failed){render.run();return;}
  try{
   if(program==0)program=createProgram();
   GL20.glUseProgram(program);GL20.glUniform1i(GL20.glGetUniformLocation(program,"Map"),0);GL20.glUniform1f(GL20.glGetUniformLocation(program,"Round"),round?1:0);GL20.glUniform1f(GL20.glGetUniformLocation(program,"Opacity"),opacity/100f);
   net.minecraft.client.render.platform.GlStateManager.disableCull();
   // The FBO is vertically flipped; use a scoped transform, as the native Mini Player does.
   canvas.push();try{canvas.translate(0,size);canvas.scale(size/(float)edge,-size/(float)edge);canvas.image(new TextureRef(Integer.valueOf(texture),edge,edge),0,0,edge,edge,0xffffffff);}finally{canvas.pop();}
  }catch(RuntimeException|LinkageError e){failed=true;OrnitheClient.LOG.warn("Minimap clipping unavailable",e);}finally{GL20.glUseProgram(oldProgram);}
 }
 private int createProgram(){int v=compile(GL20.GL_VERTEX_SHADER,"#version 120\nvarying vec2 uv;void main(){gl_Position=gl_ModelViewProjectionMatrix*gl_Vertex;uv=gl_MultiTexCoord0.xy;}");int f=compile(GL20.GL_FRAGMENT_SHADER,"#version 120\nuniform sampler2D Map;uniform float Round;uniform float Opacity;varying vec2 uv;void main(){vec4 c=texture2D(Map,uv);float edge=Round<0.5?1.0:1.0-smoothstep(0.485,0.5,length(uv-vec2(0.5)));gl_FragColor=vec4(c.rgb,c.a*edge*Opacity);}");int p=GL20.glCreateProgram();try{GL20.glAttachShader(p,v);GL20.glAttachShader(p,f);GL20.glLinkProgram(p);if(GL20.glGetProgrami(p,GL20.GL_LINK_STATUS)==0)throw new IllegalStateException(GL20.glGetProgramInfoLog(p,4096));return p;}finally{GL20.glDeleteShader(v);GL20.glDeleteShader(f);}}
 private int compile(int type,String source){int shader=GL20.glCreateShader(type);GL20.glShaderSource(shader,source);GL20.glCompileShader(shader);if(GL20.glGetShaderi(shader,GL20.GL_COMPILE_STATUS)==0)throw new IllegalStateException(GL20.glGetShaderInfoLog(shader,4096));return shader;}
 private void releaseTarget(){if(fbo!=0)GL30.glDeleteFramebuffers(fbo);if(texture!=0)GL11.glDeleteTextures(texture);fbo=texture=0;edge=0;}
 public void close(){releaseTarget();if(program!=0)GL20.glDeleteProgram(program);program=0;updated=0;}
}
