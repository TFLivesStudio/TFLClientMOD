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
   case 7->{shot("settings");c.mc.openScreen(null);try{var type=Class.forName("dev.rdh.argentum.impl.gui.VideoOptionsScreen");var constructor=type.getConstructor(net.minecraft.client.gui.screen.Screen.class);c.mc.openScreen((net.minecraft.client.gui.screen.Screen)constructor.newInstance((Object)null));}catch(ClassNotFoundException absent){finish(null);phase=99;return;}phase++;wait=20;}
   case 9->{shot("screenshot-editor");c.mc.openScreen(new CoreScreen(new dev.theredstonee.trsclient.core.ui.clips.ClipEditorUi(c.mc.gameDir.toPath().resolve("clips/native-editor-test.mp4"),16667,()->c.mc.openScreen(null)),false));phase=10;wait=10;}
   case 10->{shot("clip-editor");c.mc.openScreen(null);c.openMenu(null);phase=7;wait=10;}
   case 8->{shot("argentum");check("Argentum remains native",c.mc.screen.getClass().getName().equals("dev.rdh.argentum.impl.gui.VideoOptionsScreen"));finish(null);phase=99;}
  }
 }catch(Throwable error){finish(error);phase=99;}}
 private void finish(Throwable error){report.addProperty("status",error==null?"passed":"failed");if(error!=null){report.addProperty("error",error.toString());error.printStackTrace();}try{Files.createDirectories(out);Files.write(out.resolve("report.json"),new GsonBuilder().setPrettyPrinting().create().toJson(report).getBytes(java.nio.charset.StandardCharsets.UTF_8));}catch(Exception e){throw new RuntimeException(e);}c.mc.stop();}
}
