//? if >=26.3 {
/*package dev.theredstonee.trsclient.dev;
import dev.theredstonee.trsclient.TrsClient;
import dev.theredstonee.trsclient.compat.Mc;
import dev.theredstonee.trsclient.core.ui.background.*;
import net.minecraft.client.Minecraft;
import com.google.gson.*;
import java.nio.file.*;
public final class UnifiedSmoke {
 private int phase,wait,cameraStage;private long at,sequence;private final long started=System.nanoTime();
 private final JsonObject report=new JsonObject();private final JsonArray checks=new JsonArray();
 public static void install(){UnifiedSmoke test=new UnifiedSmoke();net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents.END_CLIENT_TICK.register(test::tick);}
 private void check(String name,boolean pass){JsonObject item=new JsonObject();item.addProperty("name",name);item.addProperty("passed",pass);checks.add(item);if(!pass)throw new AssertionError(name);}
 private void tick(Minecraft mc){if(phase==99||wait-->0)return;try{
  if(System.nanoTime()-started>240_000_000_000L)throw new AssertionError("unified timeout phase "+phase);
  var client=TrsClient.get();var modules=client.modules();
  switch(phase){
   case 0:
    if(Mc.overlay()!=null||Mc.screen()==null)return;
    mc.options.pauseOnLostFocus=false;modules.dynamicFps.setEnabled(false);org.lwjgl.sdl.SDLVideo.SDL_SetWindowSize(Mc.window().handle(),1920,1080);mc.options.guiScale().set(4);mc.resizeGui();modules.titleAnimated.set(true);modules.menuBackground.set(MenuBackground.Mode.HIGH);phase++;wait=40;return;
   case 1:
    if(MenuBackground.shared().metrics().framesPresented<2)return;at=System.nanoTime();sequence=MenuBackground.shared().metrics().framesPresented;AutoTest.shot(mc,"unified-video-a");phase++;return;
   case 2:
    if(System.nanoTime()-at<12_000_000_000L)return;
    var video=MenuBackground.shared().metrics();report.add("video",new Gson().toJsonTree(video));report.addProperty("measuredVideoFps",(video.framesPresented-sequence)*1e9/(System.nanoTime()-at));check("real video advances",video.framesPresented-sequence>80);check("source targets 30 fps",MenuBackground.Mode.HIGH.fps==30);AutoTest.shot(mc,"unified-video-b");modules.minimap.setEnabled(true);AutoTest.startWorld(mc);phase++;return;
   case 3:
    if(mc.level==null||mc.player==null||Mc.overlay()!=null||Mc.screen()!=null)return;WorldInsightProbe.run(mc,client);check("World Insight targets and player health privacy",true);check("Managed DH updater disabled",!com.seibel.distanthorizons.core.config.Config.Client.Advanced.AutoUpdater.enableAutoUpdater.get()&&!com.seibel.distanthorizons.core.config.Config.Client.Advanced.AutoUpdater.enableSilentUpdates.get());mc.options.guiScale().set(4);mc.resizeGui();check("Diorama session created",dev.diorama.MapSession.get()!=null);while(dev.diorama.Projection.turns()!=0)dev.diorama.WorldKey.rotateCamera();Mc.setScreen(new dev.diorama.MapScreen());phase++;at=System.nanoTime();wait=0;return;
   case 4:
    if(System.nanoTime()-at<15_000_000_000L)return;
    check("Diorama owns full map",Mc.screen() instanceof dev.diorama.MapScreen);AutoTest.shot(mc,"unified-diorama-map");cameraStage=0;phase=80;at=System.nanoTime();return;
   case 80:
    if(System.nanoTime()-at<6_000_000_000L||dev.diorama.MapSession.get()==null)return;
    check("cardinal map camera "+cameraStage,dev.diorama.Projection.turns()==cameraStage&&dev.diorama.MapSession.get().map().rotation()==cameraStage);AutoTest.shot(mc,"unified-camera-"+(cameraStage*90));
    double cu=dev.diorama.Projection.u(mc.player.getX(),mc.player.getZ()),cv=dev.diorama.Projection.v(mc.player.getX(),mc.player.getY(),mc.player.getZ());
    var tile=new dev.diorama.bake.TileKey(dev.diorama.bake.TileKey.SURFACE,(int)Math.floor(cu/128),(int)Math.floor(cv/128));var pick=new dev.diorama.bake.MapPick(dev.diorama.MapSession.get().map(),dev.diorama.bake.BlockLooks.get(),tile,false);
    int result=pick.pick(cu,cv);var point=result==dev.diorama.bake.MapPick.FOUND?dev.diorama.Projection.worldPos(new net.minecraft.core.BlockPos((int)Math.floor(cu),pick.y,pick.z)):null;
    JsonObject picked=new JsonObject();picked.addProperty("result",result);if(point!=null){picked.addProperty("x",point.getX());picked.addProperty("y",point.getY());picked.addProperty("z",point.getZ());picked.addProperty("rayErrorU",Math.abs(dev.diorama.Projection.u(point.getX()+.5,point.getZ()+.5)-cu));picked.addProperty("rayErrorV",Math.abs(dev.diorama.Projection.v(point.getX()+.5,point.getY(),point.getZ()+.5)-cv));}report.add("cameraPick"+cameraStage,picked);
    check("cardinal map pick converts to real world "+cameraStage,point!=null&&Math.abs(dev.diorama.Projection.u(point.getX()+.5,point.getZ()+.5)-cu)<=1&&Math.abs(dev.diorama.Projection.v(point.getX()+.5,point.getY(),point.getZ()+.5)-cv)<=1.5);
    dev.diorama.WorldKey.rotateCamera();cameraStage++;if(cameraStage<4){Mc.setScreen(new dev.diorama.MapScreen());at=System.nanoTime();return;}
    Mc.setScreen(new dev.diorama.SettingsScreen(new dev.diorama.MapScreen()));phase=5;wait=15;return;
   case 5:
    AutoTest.shot(mc,"unified-map-settings");modules.keystrokes.setEnabled(true);Mc.setScreen(new net.minecraft.client.gui.screens.PauseScreen(true));phase++;wait=15;return;
   case 6:
    check("shared pause rail",Mc.screen() instanceof dev.theredstonee.trsclient.screen.PauseChatScreen);
    var nativeChat=(dev.theredstonee.trsclient.screen.PauseChatScreen)Mc.screen();
    check("pause has exactly six primary buttons",nativeChat.actions.primaryCount()==6);check("pause retains native secondary actions",nativeChat.actions.secondaryCount()>1);
    dev.theredstonee.trsclient.TrsClient.get().hud().render(null);dev.theredstonee.trsclient.TrsClient.get().hud().renderHiddenMini(null);check("pause suppresses enabled custom HUD including F1 Mini Player",modules.keystrokes.isEnabled()&&modules.minimap.isEnabled());
    var field=net.minecraft.client.gui.screens.ChatScreen.class.getDeclaredField("input");field.setAccessible(true);check("pause input is in lower right",((net.minecraft.client.gui.components.EditBox)field.get(nativeChat)).getX()>=nativeChat.width/2);((net.minecraft.client.gui.components.EditBox)field.get(nativeChat)).setValue("/help");
    nativeChat.keyPressed(new net.minecraft.client.input.KeyEvent(com.mojang.blaze3d.platform.InputConstants.KEY_TAB,com.mojang.blaze3d.platform.InputConstants.KEYCODE_TAB,com.mojang.blaze3d.platform.InputConstants.MOD_CONTROL));
    check("SDL Ctrl Tab focuses menu",!((net.minecraft.client.gui.components.EditBox)field.get(nativeChat)).isFocused());
    nativeChat.keyPressed(new net.minecraft.client.input.KeyEvent(com.mojang.blaze3d.platform.InputConstants.KEY_TAB,com.mojang.blaze3d.platform.InputConstants.KEYCODE_TAB,0));
    check("SDL Tab returns to chat",((net.minecraft.client.gui.components.EditBox)field.get(nativeChat)).isFocused());
    nativeChat.keyPressed(new net.minecraft.client.input.KeyEvent(com.mojang.blaze3d.platform.InputConstants.KEY_RETURN,com.mojang.blaze3d.platform.InputConstants.KEYCODE_RETURN,0));
    check("chat submission retains pause",Mc.screen() instanceof dev.theredstonee.trsclient.screen.PauseChatScreen);check("native chat history retained",mc.gui.hud.getChat().getRecentChat().contains("/help"));
    phase=46;wait=10;return;
   case 46:
    mc.gui.hud.getChat().addClientSystemMessage(net.minecraft.network.chat.Component.literal("Click to suggest /help — Minecraft ñá 123").withStyle(style->style.withClickEvent(new net.minecraft.network.chat.ClickEvent.SuggestCommand("/help"))));phase=47;wait=5;return;
   case 47:
    var pause=(dev.theredstonee.trsclient.screen.PauseChatScreen)Mc.screen();boolean clickable=false;
    AutoTest.shot(mc,"unified-pause");
    outer:for(int y=(int)(pause.height*.65);y<pause.height-18;y+=2)for(int x=dev.theredstonee.trsclient.core.ui.menus.PauseLayout.chatLeft(pause.width);x<pause.width-10;x+=2){var finder=new net.minecraft.client.gui.ActiveTextCollector.ClickableStyleFinder(mc.font,x,y);mc.gui.hud.getChat().captureClickableText(finder,pause.height,mc.gui.hud.getGuiTicks(),net.minecraft.client.gui.components.ChatComponent.DisplayMode.FOREGROUND);if(finder.result()!=null&&finder.result().getClickEvent()!=null){clickable=true;report.addProperty("chatHitX",x);report.addProperty("chatHitY",y);break outer;}}
    check("native clickable chat follows lower-right layout",clickable);AutoTest.shot(mc,"unified-pause");check("SDL left click reaches full-window X",pause.mouseClicked(new net.minecraft.client.input.MouseButtonEvent(pause.width-18,16,new net.minecraft.client.input.MouseButtonInfo(dev.theredstonee.trsclient.compat.Keys.MOUSE_LEFT,0)),false));Mc.setScreen(null);mc.reloadResourcePacks();phase=7;wait=40;return;
   case 7:
    if(Mc.overlay()!=null)return;check("reload completed",true);Mc.setScreen(new dev.diorama.MapScreen());phase++;at=System.nanoTime();wait=0;return;
   case 8:
    if(System.nanoTime()-at<4_000_000_000L)return;
    AutoTest.shot(mc,"unified-diorama-reload");check("Diorama remains available after reload",dev.diorama.MapSession.get()!=null);
    try{var type=Class.forName("net.caffeinemc.mods.sodium.client.gui.VideoSettingsScreen");var method=type.getMethod("createScreen",net.minecraft.client.gui.screens.Screen.class);Mc.setScreen((net.minecraft.client.gui.screens.Screen)method.invoke(null,Mc.screen()));phase++;wait=20;return;}catch(ClassNotFoundException absent){Mc.setScreen(new net.minecraft.client.gui.screens.options.VideoSettingsScreen(Mc.screen(),mc,mc.options));phase=9;wait=20;return;}
   case 9:
    AutoTest.shot(mc,"unified-sodium");check("Unified graphics with native provider",Mc.screen() instanceof dev.theredstonee.trsclient.screen.TrsUiScreen ui&&ui.ui() instanceof dev.theredstonee.trsclient.core.ui.graphics.GraphicsUi);
    var graphics=(dev.theredstonee.trsclient.core.ui.graphics.GraphicsUi)((dev.theredstonee.trsclient.screen.TrsUiScreen)Mc.screen()).ui();report.add("graphicsInventory",new Gson().toJsonTree(graphics.inventory()));java.util.Set<String> ids=new java.util.HashSet<>();for(var option:graphics.provider().options()){check("unique graphics option "+option.id(),ids.add(option.id()));check("graphics value "+option.id(),option.value()!=null);}
    for(var option:graphics.provider().options())if(option.name().toLowerCase(java.util.Locale.ROOT).contains("bobbing")&&option.available()){String before=option.value();option.step(1);graphics.provider().apply();check("native graphics apply",!before.equals(option.value()));option.step(1);graphics.provider().apply();check("graphics preference restored",before.equals(option.value()));break;}
    AutoTest.shot(mc,"unified-graphics");phase=10;return;
   case 10:
    var shots=dev.theredstonee.trsclient.core.screenshot.Screenshots.get();check("local screenshot editor available",shots!=null);shots.platform().openUi("Screenshot editor",new dev.theredstonee.trsclient.core.ui.screenshot.ScreenshotEditorUi(shots,mc.gameDirectory.toPath().resolve("screenshots/unified-diorama-map.png")));phase++;wait=30;return;
   case 11:
    AutoTest.shot(mc,"unified-screenshot-editor");dev.theredstonee.trsclient.core.screenshot.Screenshots.get().platform().openUi("Clip editor",new dev.theredstonee.trsclient.core.ui.clips.ClipEditorUi(mc.gameDirectory.toPath().resolve("clips/native-editor-test.mp4"),16667,()->Mc.setScreen(null)));phase++;wait=15;return;
   case 12:
    AutoTest.shot(mc,"unified-clip-editor");Mc.setScreen(new dev.theredstonee.trsclient.screen.TrsUiScreen("TFL Client",new dev.theredstonee.trsclient.core.ui.menu.ModMenu(new dev.theredstonee.trsclient.screen.TrsMenuHost(null))));phase=40;wait=40;return;
   case 40:
    dev.theredstonee.trsclient.core.performance.UiProfiler.start();at=System.nanoTime();phase=41;return;
   case 41:
    if(System.nanoTime()-at<8_000_000_000L)return;report.add("rightShiftProfile",new Gson().toJsonTree(dev.theredstonee.trsclient.core.performance.UiProfiler.stop()));AutoTest.shot(mc,"unified-right-shift-profile");Mc.setScreen(null);phase=42;wait=40;return;
   case 42:
    dev.theredstonee.trsclient.core.performance.UiProfiler.start();at=System.nanoTime();phase=43;return;
   case 43:
    if(System.nanoTime()-at<8_000_000_000L)return;var worldProfile=dev.theredstonee.trsclient.core.performance.UiProfiler.stop();check("world callback profiling records real frames",((Number)worldProfile.get("worldFrames")).intValue()>0);report.add("worldWithoutMenuProfile",new Gson().toJsonTree(worldProfile));finish(mc,null);phase=99;return;
  }
 }catch(Throwable error){finish(mc,error);phase=99;}}
 private void finish(Minecraft mc,Throwable error){report.addProperty("status",error==null?"passed":"failed");report.add("checks",checks);if(error!=null){report.addProperty("error",error.toString());error.printStackTrace();}try{Files.write(mc.gameDirectory.toPath().resolve("unified-smoke.json"),new GsonBuilder().setPrettyPrinting().create().toJson(report).getBytes(java.nio.charset.StandardCharsets.UTF_8));}catch(Exception e){throw new RuntimeException(e);}mc.stop();}
}
*///?}
