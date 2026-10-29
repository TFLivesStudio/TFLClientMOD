//? if >=26.3 {
/*package dev.theredstonee.trsclient.ui;
import dev.theredstonee.trsclient.core.ui.background.LocalMjpegPlayer;
import org.lwjgl.stb.STBImage;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.system.MemoryUtil;
// Minecraft's own STB library supports JPEG; NativeImage.read deliberately accepts PNG only.
public final class NativeVideoDecoder {
 private NativeVideoDecoder(){}
 public static void install(){LocalMjpegPlayer.installNativeDecoder((jpeg,maxW,maxH,pixels)->{
  java.nio.ByteBuffer encoded=MemoryUtil.memAlloc(jpeg.length),decoded=null;
  try(MemoryStack stack=MemoryStack.stackPush()){
   encoded.put(jpeg).flip();var width=stack.mallocInt(1);var height=stack.mallocInt(1);var channels=stack.mallocInt(1);
   if(!STBImage.stbi_info_from_memory(encoded,width,height,channels))throw new java.io.IOException("Invalid JPEG header");
   int sw=width.get(0),sh=height.get(0);
   if(sw<=0||sh<=0||sw>4096||sh>4096||(long)sw*sh>8388608)throw new java.io.IOException("Video dimensions exceed bound");
   decoded=STBImage.stbi_load_from_memory(encoded,width,height,channels,4);
   if(decoded==null)throw new java.io.IOException(STBImage.stbi_failure_reason());
   double scale=Math.min(1,Math.min(maxW/(double)sw,maxH/(double)sh));int w=Math.max(1,(int)(sw*scale)),h=Math.max(1,(int)(sh*scale)),count=w*h;
   if(pixels.length<count)throw new java.io.IOException("Unexpected video dimensions");
   java.nio.IntBuffer nativePixels=decoded.order(java.nio.ByteOrder.LITTLE_ENDIAN).asIntBuffer();
   if(scale==1)nativePixels.get(pixels,0,count);
   else for(int y=0;y<h;y++)for(int x=0;x<w;x++){
    double fx=(x+.5)*sw/w-.5,fy=(y+.5)*sh/h-.5;int x0=Math.max(0,(int)Math.floor(fx)),y0=Math.max(0,(int)Math.floor(fy)),x1=Math.min(sw-1,x0+1),y1=Math.min(sh-1,y0+1);double tx=Math.max(0,fx-x0),ty=Math.max(0,fy-y0);int a=nativePixels.get(y0*sw+x0),b=nativePixels.get(y0*sw+x1),c=nativePixels.get(y1*sw+x0),d=nativePixels.get(y1*sw+x1),out=0xff000000;
    for(int shift=0;shift<=16;shift+=8){double top=(a>>>shift&255)*(1-tx)+(b>>>shift&255)*tx,bottom=(c>>>shift&255)*(1-tx)+(d>>>shift&255)*tx;out|=(int)Math.round(top*(1-ty)+bottom*ty)<<shift;}pixels[y*w+x]=out;
   }
   for(int i=0;i<count;i++){int p=pixels[i];pixels[i]=(p&0xff00ff00)|(p&255)<<16|(p>>>16&255);}
   return new LocalMjpegPlayer.PixelFrame(w,h,pixels);
  }finally{if(decoded!=null)STBImage.stbi_image_free(decoded);MemoryUtil.memFree(encoded);}
 });}
}
*///?}
