//? if >=26.3 {
/*package dev.theredstonee.trsclient.dev;
import dev.theredstonee.trsclient.TrsClient;
import dev.theredstonee.trsclient.compat.Mc;
import dev.theredstonee.trsclient.core.hud.miniplayer.MiniPlayerModule;
import dev.theredstonee.trsclient.core.map.WorldMapUi;
import dev.theredstonee.trsclient.core.module.HudModule;
import dev.theredstonee.trsclient.hud.MiniPlayerHud;
import dev.theredstonee.trsclient.hud.MiniPlayerRenderer;
import dev.theredstonee.trsclient.screen.*;
import net.minecraft.client.Minecraft;
import java.nio.file.*;
import com.google.gson.*;
// Native integration probe, excluded from distributable JARs along with other dev classes.
public final class DepthSmoke {
 private int phase,wait,mapStep;private long at,updatesBefore;private double x,z;private int perspective;private double fov;
 private java.util.concurrent.CompletableFuture<Void> reload;private Object oldPlayer;
 private MiniPlayerHud mini;private WorldMapUi map;private final JsonArray checks=new JsonArray();
 private final JsonObject report=new JsonObject();private final long start=System.currentTimeMillis();
 public static void install(){DepthSmoke s=new DepthSmoke();if("depthrestart".equals(System.getProperty("trsclient.autotest.only")))s.phase=300;net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents.END_CLIENT_TICK.register(s::tick);}
 private void check(String name,boolean pass){JsonObject c=new JsonObject();c.addProperty("name",name);c.addProperty("passed",pass);checks.add(c);if(!pass)throw new AssertionError(name);}
 private void shot(Minecraft mc,String name){AutoTest.shot(mc,"depth-"+name);}
 private void tick(Minecraft mc){
  if(wait-->0)return;
  try{
   if(System.currentTimeMillis()-start>240000)throw new AssertionError("Depth native timeout, phase "+phase);
   var client=TrsClient.get();var m=client.modules();MiniPlayerModule doll=m.miniPlayer;
   switch(phase){
    case 300:
     if(Mc.overlay()!=null||Mc.screen()==null)return;
     check("Mini Player preferences survive process restart",doll.opacity.getInt()==65&&doll.angle.get()==MiniPlayerModule.Angle.SIDE&&Math.abs(doll.position().offsetX-.123)<1e-8);shot(mc,"restart-title");finish(mc,null);phase=99;return;
    case 0:
     if(Mc.overlay()!=null||Mc.screen()==null)return;
     mc.options.pauseOnLostFocus=false;mc.options.renderDistance().set(4);
     org.lwjgl.sdl.SDLVideo.SDL_SetWindowSize(Mc.window().handle(),1920,1080);mc.options.guiScale().set(4);mc.resizeGui();
     for(var module:m.registry.all())if(module instanceof HudModule)module.setEnabled(false);
     doll.reset();doll.setEnabled(true);doll.scale.set(1);doll.position().offsetX=.02;doll.position().offsetY=.02;
     for(var h:client.hud().elements())if(h instanceof MiniPlayerHud d)mini=d;
     check("Mini Player registered in HUD and editor",mini!=null&&client.hud().editorItems().stream().anyMatch(h->h.module()==doll));
     AutoTest.startWorld(mc);phase++;return;
    case 1:
     if(mc.player==null||mc.level==null||Mc.screen()!=null)return;
     AutoTest.command(mc,"clear @p");
     AutoTest.command(mc,"fill -16 75 -16 16 75 16 minecraft:stone");
     AutoTest.command(mc,"fill -16 76 -16 16 85 16 minecraft:air");
     AutoTest.command(mc,"tp @p 0.5 76 0.5 0 0");phase=101;wait=20;return;
    case 101:
     mc.options.guiScale().set(4);mc.resizeGui();
     perspective=mc.options.getCameraType().ordinal();fov=mc.options.fov().get();at=System.nanoTime();phase=2;wait=30;return;
    case 2:
     check("native player PIP rendered",MiniPlayerRenderer.rendered()>0&&MiniPlayerRenderer.composited()>0);
     check("native widget state has no failure",mini.state.failure.isEmpty());
     check("world camera unchanged by widget",perspective==mc.options.getCameraType().ordinal()&&fov==mc.options.fov().get());
     shot(mc,"mini-front");report.addProperty("nativeSkin",mc.player.getSkin().toString());
     AutoTest.command(mc,"item replace entity @p armor.head with minecraft:diamond_helmet");
     AutoTest.command(mc,"item replace entity @p armor.chest with minecraft:diamond_chestplate");
     AutoTest.command(mc,"item replace entity @p weapon.mainhand with minecraft:diamond_sword");
     phase++;wait=25;return;
    case 3:
     check("native equipment received",!mc.player.getItemBySlot(net.minecraft.world.entity.EquipmentSlot.HEAD).isEmpty()&&!mc.player.getMainHandItem().isEmpty());shot(mc,"mini-equipment");doll.armor.set(false);doll.heldItems.set(false);phase++;wait=15;return;
    case 4:
     shot(mc,"mini-hidden-equipment");doll.armor.set(true);doll.heldItems.set(true);doll.angle.set(MiniPlayerModule.Angle.THREE_QUARTERS);
     x=mc.player.getX();z=mc.player.getZ();mc.options.keyUp.setDown(true);phase++;wait=20;return;
    case 5:
     check("native movement reached real player",Math.abs(mc.player.getX()-x)+Math.abs(mc.player.getZ()-z)>.1);shot(mc,"mini-walking");mc.options.keySprint.setDown(true);phase++;wait=15;return;
    case 6:
     shot(mc,"mini-running");mc.options.keyUp.setDown(false);mc.options.keySprint.setDown(false);mc.options.keyJump.setDown(true);phase++;wait=3;return;
    case 7:
     shot(mc,"mini-jump");mc.options.keyJump.setDown(false);mc.options.keyShift.setDown(true);phase++;wait=10;return;
    case 8:
     shot(mc,"mini-sneak");mc.options.keyShift.setDown(false);doll.angle.set(MiniPlayerModule.Angle.FRONT);doll.opacity.set(55);phase++;wait=15;return;
    case 9:
     shot(mc,"mini-opacity");doll.opacity.set(100);
     mc.player.swingAndResetAttackStrength(net.minecraft.world.InteractionHand.MAIN_HAND,net.minecraft.world.item.component.SwingAnimation.DEFAULT,true);phase=200;wait=2;return;
    case 200:
     shot(mc,"mini-attack");m.emotes.setEnabled(true);m.emoteCamera.set(dev.theredstonee.trsclient.core.module.TrsModules.EmoteCamera.OFF);
     check("native local emote starts",dev.theredstonee.trsclient.online.EmoteHooks.emotes().play("winken",System.currentTimeMillis())==dev.theredstonee.trsclient.core.emote.EmoteController.Result.PLAYED);phase=201;wait=10;return;
    case 201:
     shot(mc,"mini-emote");dev.theredstonee.trsclient.online.EmoteHooks.emotes().onAttack(System.currentTimeMillis());doll.setEnabled(false);updatesBefore=mini.state.updates;phase=10;wait=15;return;
    case 10:
     check("disabled widget skips extraction",mini.state.updates==updatesBefore);doll.setEnabled(true);doll.hideMenus.set(false);Mc.setScreen(MenusTest.options(new TrsTitleScreen(),mc));phase++;wait=15;return;
    case 11:
     shot(mc,"mini-in-menu");Mc.setScreen(null);doll.hideMenus.set(true);m.worldMap.setEnabled(true);m.mapCloudFog.set(true);m.worldMapTilt.set(0);m.worldMapRotation.set(0);var screen=WorldMapScreen.create();check("world map available",screen!=null);map=(WorldMapUi)screen.ui();Mc.setScreen(screen);phase++;wait=30;return;
    case 12:
     if(map.volumeRenderer().displayed()==null){wait=5;return;}
     map.mouseScrolled(Mc.screen().width/2.,Mc.screen().height/2.,-3);phase++;wait=30;return;
    case 13:
     var volume=map.volumeRenderer();check("3D volume native backend healthy",volume.failure().isEmpty());
     if(volume.displayed()==null){wait=5;return;}
     double target=mapStep==0?0:mapStep==1?30:55;
     if(Math.abs(volume.displayed().tilt-target)>.1){wait=5;return;}
     shot(mc,"map-"+mapStep);var camera=volume.displayed();double[] pick=map.testWorldAt(camera.width/2,camera.height/2);
     check("native perspective picking "+mapStep,Math.abs(pick[0]-camera.cx)<.01&&Math.abs(pick[1]-camera.cz)<.01);
     if(++mapStep<3){m.worldMapTilt.set(mapStep==1?30:55);wait=30;return;}
     report.addProperty("volumeAverageMs",volume.renderNanos()/1e6/Math.max(1,volume.renderedFrames()));
     report.addProperty("volumeBackend",volume.backend());Mc.setScreen(null);check("closed map releases display projection",volume.displayed()==null);phase++;wait=10;return;
    case 14:
     updatesBefore=mini.state.updates;mini.invalidate();phase++;wait=15;return;
    case 15:
     check("widget recovers after attachment invalidation",mini.state.updates>updatesBefore);shot(mc,"mini-recreated");
     report.addProperty("miniUpdates",mini.state.updates);report.addProperty("miniAverageExtractionMs",mini.state.nanos/1e6/Math.max(1,mini.state.updates));report.addProperty("pipRenders",MiniPlayerRenderer.rendered());report.addProperty("pipComposites",MiniPlayerRenderer.composited());reload=mc.reloadResourcePacks();phase++;wait=20;return;
    case 16:
     if(!reload.isDone()||Mc.overlay()!=null){wait=5;return;}reload.join();phase++;wait=20;return;
    case 17:
     check("native resource reload recreates model",mini.state.failure.isEmpty()&&MiniPlayerRenderer.rendered()>0);shot(mc,"mini-after-resource-reload");
     mc.gui.hud.toggle();check("F1 enabled",Mc.hudHidden());updatesBefore=mini.state.updates;phase++;wait=10;return;
    case 18:
     check("F1 default skips model extraction",mini.state.updates==updatesBefore);doll.hideF1.set(false);phase++;wait=10;return;
    case 19:
     check("F1 opt-in draws model",mini.state.updates>updatesBefore);shot(mc,"mini-F1-opt-in");mc.gui.hud.toggle();doll.hideF1.set(true);oldPlayer=mc.player;AutoTest.command(mc,"kill @p");phase++;wait=20;return;
    case 20:
     if(!mc.player.isDeadOrDying()){wait=5;return;}shot(mc,"mini-death");mc.player.respawn();phase++;wait=30;return;
    case 21:
     if(mc.player==oldPlayer||mc.player.isDeadOrDying()||Mc.screen()!=null){wait=5;return;}
     check("server respawn replaced native player",mc.player!=oldPlayer);shot(mc,"mini-respawn");AutoTest.command(mc,"execute as @a in minecraft:the_nether run tp @s 0 80 0");phase++;wait=30;return;
    case 22:
     if(!mc.level.dimension().equals(net.minecraft.world.level.Level.NETHER)||Mc.screen()!=null){wait=5;return;}check("native Nether change received",true);shot(mc,"mini-nether");AutoTest.command(mc,"execute as @a in minecraft:overworld run tp @s 0 80 0");phase++;wait=30;return;
    case 23:
     if(!mc.level.dimension().equals(net.minecraft.world.level.Level.OVERWORLD)||Mc.screen()!=null){wait=5;return;}check("native return dimension received",true);shot(mc,"mini-return-world");finish(mc,null);phase=99;return;
   }
  }catch(Throwable error){finish(mc,error);phase=99;}
 }
 private void finish(Minecraft mc,Throwable failure){
  if(failure==null&&phase!=300){var client=TrsClient.get();client.modules().miniPlayer.opacity.set(65);client.modules().miniPlayer.angle.set(MiniPlayerModule.Angle.SIDE);client.modules().miniPlayer.position().offsetX=.123;client.saveConfig();dev.theredstonee.trsclient.core.config.ConfigStore.active().flush();}
  report.addProperty("status",failure==null?"passed":"failed");report.add("checks",checks);report.addProperty("elapsedMs",System.currentTimeMillis()-start);
  if(failure!=null){report.addProperty("error",failure.toString());TrsClient.LOGGER.error("Depth native probe failed",failure);}
  try{Files.write(mc.gameDirectory.toPath().resolve(phase==300?"depth-restart.json":"depth-smoke.json"),new GsonBuilder().setPrettyPrinting().create().toJson(report).getBytes(java.nio.charset.StandardCharsets.UTF_8));}catch(Exception e){throw new RuntimeException(e);}mc.stop();
 }
}

*///?}
