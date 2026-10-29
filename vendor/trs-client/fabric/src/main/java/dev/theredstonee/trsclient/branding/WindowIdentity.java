package dev.theredstonee.trsclient.branding;
public final class WindowIdentity {
 private WindowIdentity(){}
 //? if >=26.3 {
 /*private static long installed;
 public static void refresh(){
  long window=dev.theredstonee.trsclient.compat.Mc.window().handle();
  String title=dev.theredstonee.trsclient.core.product.Product.displayName("26.3");
  if(!title.equals(org.lwjgl.sdl.SDLVideo.SDL_GetWindowTitle(window)))org.lwjgl.sdl.SDLVideo.SDL_SetWindowTitle(window,title);
  if(window==installed)return;installed=window;
  java.nio.ByteBuffer bytes=org.lwjgl.system.MemoryUtil.memAlloc(128*128*4);
  org.lwjgl.sdl.SDL_Surface surface=null;
  try{
   for(int p:dev.theredstonee.trsclient.core.branding.BrandIcons.pixels(128))bytes.put((byte)(p>>16)).put((byte)(p>>8)).put((byte)p).put((byte)(p>>>24));bytes.flip();
   surface=org.lwjgl.sdl.SDLSurface.SDL_CreateSurfaceFrom(128,128,org.lwjgl.sdl.SDLPixels.SDL_PIXELFORMAT_RGBA32,bytes,128*4);
   if(surface==null||!org.lwjgl.sdl.SDLVideo.SDL_SetWindowIcon(window,surface))dev.theredstonee.trsclient.TrsClient.LOGGER.warn("SDL could not set the TFL window icon");
  }catch(RuntimeException failure){dev.theredstonee.trsclient.TrsClient.LOGGER.warn("TFL icon unavailable",failure);}finally{if(surface!=null)org.lwjgl.sdl.SDLSurface.SDL_DestroySurface(surface);org.lwjgl.system.MemoryUtil.memFree(bytes);}
 }
 *///?} else {
 public static void refresh(){}
 //?}
}
