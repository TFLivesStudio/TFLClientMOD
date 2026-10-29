package dev.theredstonee.trsclient.ornithe;
import org.lwjgl.opengl.*;
import org.lwjgl.BufferUtils;
/** First native loading frame, before game fonts and client initialization exist. */
public final class BootScreen {
 private BootScreen(){}
 public static void draw(){
  WindowIdentity.refresh();
  int w=Display.getWidth(),h=Display.getHeight();
  GL11.glViewport(0,0,w,h);GL11.glClearColor(8/255f,13/255f,24/255f,1);GL11.glClear(GL11.GL_COLOR_BUFFER_BIT|GL11.GL_DEPTH_BUFFER_BIT);
  int old=GL11.glGetInteger(GL11.GL_MATRIX_MODE),binding=GL11.glGetInteger(GL11.GL_TEXTURE_BINDING_2D),texture=0;
  GL11.glPushAttrib(GL11.GL_ALL_ATTRIB_BITS);
  GL11.glMatrixMode(GL11.GL_PROJECTION);GL11.glPushMatrix();GL11.glLoadIdentity();GL11.glOrtho(0,w,h,0,-1,1);
  GL11.glMatrixMode(GL11.GL_MODELVIEW);GL11.glPushMatrix();GL11.glLoadIdentity();
  try(var stream=BootScreen.class.getResourceAsStream("/assets/trsclient/textures/ui/boot-wordmark.png")){
   var image=javax.imageio.ImageIO.read(stream);var bytes=BufferUtils.createByteBuffer(image.getWidth()*image.getHeight()*4);
   for(int y=0;y<image.getHeight();y++)for(int x=0;x<image.getWidth();x++){int p=image.getRGB(x,y);bytes.put((byte)(p>>16)).put((byte)(p>>8)).put((byte)p).put((byte)(p>>24));}bytes.flip();
   texture=GL11.glGenTextures();GL11.glBindTexture(GL11.GL_TEXTURE_2D,texture);GL11.glTexParameteri(GL11.GL_TEXTURE_2D,GL11.GL_TEXTURE_MIN_FILTER,GL11.GL_LINEAR);GL11.glTexParameteri(GL11.GL_TEXTURE_2D,GL11.GL_TEXTURE_MAG_FILTER,GL11.GL_LINEAR);
   GL11.glTexImage2D(GL11.GL_TEXTURE_2D,0,GL11.GL_RGBA8,image.getWidth(),image.getHeight(),0,GL11.GL_RGBA,GL11.GL_UNSIGNED_BYTE,bytes);
   GL11.glDisable(GL11.GL_DEPTH_TEST);GL11.glEnable(GL11.GL_TEXTURE_2D);GL11.glEnable(GL11.GL_BLEND);GL11.glBlendFunc(GL11.GL_SRC_ALPHA,GL11.GL_ONE_MINUS_SRC_ALPHA);GL11.glColor4f(1,1,1,1);
   float width=Math.min(w*.6f,520),height=width*image.getHeight()/image.getWidth(),x=(w-width)/2,y=(h-height)/2;
   GL11.glBegin(GL11.GL_QUADS);GL11.glTexCoord2f(0,0);GL11.glVertex2f(x,y);GL11.glTexCoord2f(1,0);GL11.glVertex2f(x+width,y);GL11.glTexCoord2f(1,1);GL11.glVertex2f(x+width,y+height);GL11.glTexCoord2f(0,1);GL11.glVertex2f(x,y+height);GL11.glEnd();
  }catch(java.io.IOException e){OrnitheClient.LOG.warn("Boot wordmark unavailable",e);}
  finally{if(texture!=0)GL11.glDeleteTextures(texture);GL11.glBindTexture(GL11.GL_TEXTURE_2D,binding);GL11.glPopMatrix();GL11.glMatrixMode(GL11.GL_PROJECTION);GL11.glPopMatrix();GL11.glMatrixMode(old);GL11.glPopAttrib();}
  Display.update();
 }
}
