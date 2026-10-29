package dev.theredstonee.trsclient.ornithe.dev;
import dev.theredstonee.trsclient.ornithe.*;
import dev.theredstonee.trsclient.core.ui.*;
import dev.theredstonee.trsclient.core.ui.background.*;
import java.nio.file.*;
import com.google.gson.*;
import net.minecraft.world.*;
import net.minecraft.world.gen.WorldGeneratorType;
import net.minecraft.client.Screenshot;
/** Updated native scenario for the unified client and the actual Diorama port. */
public final class UnifiedSmoke implements Runnable {
 private final OrnitheClient c;private final Path out;private final JsonObject report=new JsonObject();private final JsonArray checks=new JsonArray();
 private String originalUpscaler;private boolean shaderPickerSeen;private net.minecraft.client.gui.screen.Screen graphicsReturn;
 private int phase,wait;private long at,sequence;private final long start=System.nanoTime();
 public UnifiedSmoke(OrnitheClient c,Path out){this.c=c;this.out=out;report.add("checks",checks);}
 private void check(String name,boolean pass){JsonObject item=new JsonObject();item.addProperty("name",name);item.addProperty("passed",pass);checks.add(item);if(!pass)throw new AssertionError(name);}
 private void shot(String name){Screenshot.take(out.toFile(),"unified-"+name+".png",c.mc.width,c.mc.height,c.mc.getRenderTarget());}
 public void run(){if(wait-->0)return;try{
  if(System.nanoTime()-start>240_000_000_000L)throw new AssertionError("unified timeout phase "+phase);
  switch(phase){
   case 0->{c.mc.options.pauseOnUnfocus=false;c.mc.options.fpsLimit=120;c.mc.options.vsync=false;c.modules.dynamicFps.setEnabled(false);for(var module:c.modules.registry.all())if(module instanceof dev.theredstonee.trsclient.core.module.HudModule)module.setEnabled(false);c.modules.miniPlayer.setEnabled(true);c.modules.fps.setEnabled(true);c.mc.options.guiScale=4;c.modules.titleAnimated.set(true);c.modules.menuBackground.set(MenuBackground.Mode.HIGH);c.mc.openScreen(c.title());wait=40;phase++;}
   case 1->{var metrics=MenuBackground.shared().metrics();if(metrics.framesPresented<2)return;sequence=metrics.framesPresented;at=System.nanoTime();shot("video-a");phase++;}
   case 2->{if(System.nanoTime()-at<12_000_000_000L)return;var metrics=MenuBackground.shared().metrics();double fps=(metrics.framesPresented-sequence)*1e9/(System.nanoTime()-at);report.addProperty("measuredVideoFps",fps);report.add("video",new Gson().toJsonTree(metrics));check("real video frames advance",metrics.framesPresented-sequence>80);check("source targets 30 fps",MenuBackground.Mode.HIGH.fps==30);shot("video-b");c.modules.minimap.setEnabled(true);c.mc.startGame("tfl-unified-smoke","TFL Diorama",new WorldSettings(189L,WorldSettings.GameMode.CREATIVE,false,false,WorldGeneratorType.FLAT).enableCommands());phase++;wait=50;}
   case 3->{if(c.mc.player==null||c.mc.world==null)return;c.mc.openScreen(null);c.mc.player.sendChat("/fill "+((int)c.mc.player.x+8)+" 5 "+(int)c.mc.player.z+" "+((int)c.mc.player.x+16)+" 20 "+((int)c.mc.player.z+8)+" minecraft:stone");check("received chunks captured",c.diorama!=null&&c.diorama.snapshotCount()>0);shot("hud");c.openWorldMap(null);phase++;wait=160;}
   case 4->{if(c.diorama.bakedTiles<2){wait=30;return;}check("original Diorama voxel tiles baked",c.diorama.bakedTiles>=2);check("Diorama worker healthy",c.diorama.failure.isEmpty());check("native Diorama fog shader linked",c.diorama.fogHealthy());check("native Diorama map owns screen",c.mc.screen instanceof CoreScreen screen&&screen.ui instanceof OrnitheDioramaUi);report.addProperty("bakedTiles",c.diorama.bakedTiles);report.addProperty("meanBakeMs",c.diorama.bakeNanos/1e6/c.diorama.bakedTiles);shot("map");c.mc.openScreen(c.navigation.pause());phase++;wait=10;}
   case 5->{check("shared pause rail",c.mc.screen instanceof CoreScreen screen&&screen.ui instanceof dev.theredstonee.trsclient.core.ui.menus.ActionListUi);shot("pause");c.mc.openScreen(null);c.navigation.captures(null);phase++;wait=10;}
   case 6->{shot("captures");var shots=dev.theredstonee.trsclient.core.screenshot.Screenshots.get();check("local screenshot editor available",shots!=null);c.mc.openScreen(new CoreScreen(new dev.theredstonee.trsclient.core.ui.screenshot.ScreenshotEditorUi(shots,out.resolve("screenshots/unified-hud.png")),false));phase=9;wait=30;}
   case 7->{shot("settings");c.mc.openScreen(null);try{var type=Class.forName("dev.rdh.argentum.impl.gui.VideoOptionsScreen");var constructor=type.getConstructor(net.minecraft.client.gui.screen.Screen.class);c.mc.openScreen((net.minecraft.client.gui.screen.Screen)constructor.newInstance((Object)null));}catch(ClassNotFoundException absent){c.mc.openScreen(new net.minecraft.client.gui.screen.menu.options.VideoOptionsScreen(null,c.mc.options));phase=18;wait=15;return;}phase++;wait=20;}
   case 9->{shot("screenshot-editor");c.mc.openScreen(new CoreScreen(new dev.theredstonee.trsclient.core.ui.clips.ClipEditorUi(c.mc.gameDir.toPath().resolve("clips/native-editor-test.mp4"),16667,()->c.mc.openScreen(null)),false));phase=10;wait=10;}
   case 10->{shot("clip-editor");c.mc.openScreen(null);c.openMenu(null);phase=7;wait=10;}
   case 8->{if(c.mc.screen instanceof CoreScreen core&&core.ui instanceof dev.theredstonee.trsclient.core.ui.graphics.GraphicsUi graphics){report.add("graphicsInventory",new Gson().toJsonTree(graphics.inventory()));
    java.util.Set<String> ids=new java.util.HashSet<>();for(var option:graphics.provider().options())check("unique graphics option "+option.id(),ids.add(option.id()));
    for(var option:graphics.provider().options())if(option.name().toLowerCase(java.util.Locale.ROOT).contains("bobbing")&&option.available()){
     String before=option.value();option.step(1);check("graphics pending state",option.changed());graphics.provider().apply();check("graphics native apply",!option.changed()&&!before.equals(option.value()));option.step(1);graphics.provider().apply();check("graphics value restored",before.equals(option.value()));break;
    }
    graphics.selectCategory(3);}shot("argentum");check("Unified graphics owns native options",c.mc.screen instanceof CoreScreen screen&&screen.ui instanceof dev.theredstonee.trsclient.core.ui.graphics.GraphicsUi);phase=16;wait=15;}
   case 16->{shot("shaders-upscaling");if(!shaderPickerSeen&&c.mc.screen instanceof CoreScreen current&&current.ui instanceof dev.theredstonee.trsclient.core.ui.graphics.GraphicsUi panel&&panel.provider().hasShaders()){shaderPickerSeen=true;graphicsReturn=c.mc.screen;panel.provider().shaders();phase=25;wait=15;return;}if(c.mc.screen instanceof CoreScreen core&&core.ui instanceof dev.theredstonee.trsclient.core.ui.graphics.GraphicsUi graphics){for(var option:graphics.provider().options())if(option.id().equals("amplio:upscaler")){originalUpscaler=option.value();for(int i=0;i<3&&!option.value().contains("DLSS");i++)option.step(1);graphics.provider().apply();phase=24;wait=30;return;}}phase=11;wait=10;}
   case 25->{check("Native shader-pack selector",c.mc.screen.getClass().getName().equals("us.z1x.lucerna.gui.screen.ShaderPackScreen"));shot("shader-packs");var iris=Class.forName("us.z1x.lucerna.Iris");Object config=iris.getMethod("getIrisConfig").invoke(null);boolean before=(Boolean)config.getClass().getMethod("areShadersEnabled").invoke(config);var toggle=iris.getMethod("toggleShaders",net.minecraft.client.Minecraft.class,boolean.class);toggle.invoke(null,c.mc,!before);check("Shader enable binding",(Boolean)config.getClass().getMethod("areShadersEnabled").invoke(config)==!before);toggle.invoke(null,c.mc,before);check("Shader preference restored",(Boolean)config.getClass().getMethod("areShadersEnabled").invoke(config)==before);c.mc.openScreen(graphicsReturn);phase=16;wait=15;}
   case 24->{var graphics=(dev.theredstonee.trsclient.core.ui.graphics.GraphicsUi)((CoreScreen)c.mc.screen).ui;String status=graphics.provider().status();report.addProperty("upscalerFallback",status);if(org.lwjgl.opengl.GL11.glGetString(org.lwjgl.opengl.GL11.GL_RENDERER).toLowerCase(java.util.Locale.ROOT).contains("llvmpipe"))check("DLSS request reports FSR1 fallback",status.contains("DLSS")&&status.contains("FSR1")&&status.contains("fallback"));shot("dlss-fallback");for(var option:graphics.provider().options())if(option.id().equals("amplio:upscaler")){for(int i=0;i<3&&!option.value().equals(originalUpscaler);i++)option.step(1);graphics.provider().apply();check("upscaler preference restored",option.value().equals(originalUpscaler));}phase=11;wait=10;}
   case 18->{check("Unified vanilla graphics",c.mc.screen instanceof CoreScreen core&&core.ui instanceof dev.theredstonee.trsclient.core.ui.graphics.GraphicsUi);shot("vanilla-graphics");phase=11;wait=10;}
   case 11->{c.mc.openScreen(new net.minecraft.client.gui.screen.menu.options.LanguageOptionsScreen(null,c.mc.options,c.mc.getLanguageManager()));phase=12;wait=15;}
   case 12->{shot("languages");c.mc.openScreen(new net.minecraft.client.gui.screen.menu.multiplayer.MultiplayerScreen(null));phase=13;wait=15;}
   case 13->{shot("multiplayer");check("public window title",org.lwjgl.opengl.Display.getTitle().equals("TFL Client 1.8.9"));c.mc.openScreen(c.title());phase=14;wait=30;}
   case 14->{shot("clean-title");c.modules.titleAnimated.set(false);c.mc.openScreen(c.title());phase=15;wait=30;}
   case 15->{shot("static-title");resize(1280,720);phase=19;wait=30;}
   case 19->{check("1280x720 framebuffer",c.mc.width==1280&&c.mc.height==720);shot("title-720p");resize(854,480);phase=20;wait=30;}
   case 20->{check("854x480 framebuffer",c.mc.width==854&&c.mc.height==480);shot("title-480p");resize(1920,1080);phase=21;wait=30;}
   case 21->{c.mc.toggleFullscreen();phase=22;wait=15;}
   case 22->{check("title in fullscreen",org.lwjgl.opengl.Display.getTitle().equals("TFL Client 1.8.9"));shot("title-fullscreen");c.mc.toggleFullscreen();phase=23;wait=15;}
   case 23->{check("title after fullscreen",org.lwjgl.opengl.Display.getTitle().equals("TFL Client 1.8.9"));c.modules.titleAnimated.set(true);finish(null);phase=99;}

  }
 }catch(Throwable error){finish(error);phase=99;}}
 private void resize(int width,int height)throws org.lwjgl.LWJGLException{org.lwjgl.opengl.Display.setDisplayMode(new org.lwjgl.opengl.DisplayMode(width,height));((dev.theredstonee.trsclient.ornithe.mixin.MinecraftResizeAccess)c.mc).tfl$resize(width,height);}
 private void finish(Throwable error){report.addProperty("status",error==null?"passed":"failed");if(error!=null){report.addProperty("error",error.toString());error.printStackTrace();}try{Files.createDirectories(out);Files.write(out.resolve("report.json"),new GsonBuilder().setPrettyPrinting().create().toJson(report).getBytes(java.nio.charset.StandardCharsets.UTF_8));}catch(Exception e){throw new RuntimeException(e);}c.mc.stop();}
}
