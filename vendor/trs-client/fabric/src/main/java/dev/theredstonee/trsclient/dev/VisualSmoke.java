//? if >=26.3 {
/*package dev.theredstonee.trsclient.dev;

import com.google.gson.*;
import dev.theredstonee.trsclient.core.ui.graphics.*;
import dev.theredstonee.trsclient.core.ui.menu.ModMenu;
import dev.theredstonee.trsclient.screen.*;
import dev.theredstonee.trsclient.compat.Mc;
import net.minecraft.client.Minecraft;
import java.nio.file.*;
import java.util.Objects;


// Focused UI-only probe; excluded from the production JAR.
public final class VisualSmoke implements Runnable {
  private final Minecraft mc=Minecraft.getInstance();
  private final Path out;
  private final JsonArray checks=new JsonArray();
  private final long started=System.nanoTime();
  private int phase,wait=60;
  private GraphicsProfiles profiles;
  private GraphicsProfiles.Plan plan;
  public VisualSmoke(){out=mc.gameDirectory.toPath();}
  public static void install(){VisualSmoke probe=new VisualSmoke();net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents.END_CLIENT_TICK.register(ignored->probe.run());}
  private void check(String name,boolean pass){JsonObject item=new JsonObject();item.addProperty("name",name);item.addProperty("passed",pass);checks.add(item);if(!pass)throw new AssertionError(name);}
  private void shot(String name){AutoTest.shot(mc,"visual-"+name);}
  public void run(){
    try {
      if(System.nanoTime()-started>100_000_000_000L)throw new AssertionError("UI probe timeout");
      if(wait-->0)return;
      System.out.println("[TFL Visual] phase "+phase);
      switch(phase++) {
        case 0 -> {shot("title");mc.options.guiScale().set(2);Mc.setScreen(new TrsUiScreen("TFL Client",new ModMenu(new TrsMenuHost(null))));}
        case 1 -> {shot("modules");Mc.setScreen(new TrsUiScreen("TFL Client",new ModMenu(new TrsMenuHost(null)).showSettingsHub()));}
        case 2 -> {shot("settings-hub");Mc.setScreen(new TrsUiScreen("TFL Client",new ModMenu(new TrsMenuHost(null)).select(dev.theredstonee.trsclient.TrsClient.get().modules().keystrokes)));}
        case 3 -> {shot("module-detail");openGraphics();}
        case 4 -> {
          check("graphics native host connected",Mc.screen() instanceof TrsUiScreen && ((TrsUiScreen)Mc.screen()).ui() instanceof GraphicsUi);
          GraphicsUi ui=(GraphicsUi)((TrsUiScreen)Mc.screen()).ui();shot("profiles-before");
          profiles=new GraphicsProfiles(ui.provider());plan=profiles.prepare(GraphicsProfiles.Profile.BALANCED);
          check("native profile exposes supported changes",!plan.changes.isEmpty());profiles.apply(plan);
          for(var change:plan.changes)check("applied "+change.option.id(),Objects.equals(change.after,change.binding.read()));
        }
        case 5 -> {shot("profiles-applied");check("native undo available",profiles.canUndo());profiles.undo();for(var change:plan.changes)check("restored "+change.option.id(),Objects.equals(change.before,change.binding.read()));}
        case 6 -> {shot("profiles-restored");resize(1280,720);Mc.setScreen(new TrsUiScreen("TFL Client",new ModMenu(new TrsMenuHost(null))));}
        case 7 -> {shot("modules-1280x720");resize(854,480);openGraphics();dev.theredstonee.trsclient.core.i18n.I18n.use("es");}
        case 8 -> {shot("profiles-854x480-es");Mc.setScreen(new TrsUiScreen("HUD Editor",new dev.theredstonee.trsclient.core.ui.menu.HudEditor(new TrsMenuHost(null))));}
        case 9 -> {shot("hud-library-854x480");finish(null);}
      }
      wait=25;
    }catch(Throwable failure){finish(failure);}
  }
  private void resize(int width,int height){Mc.window().setWindowed(width,height);}
  private void openGraphics()throws Exception {
    try {var type=Class.forName("net.caffeinemc.mods.sodium.client.gui.VideoSettingsScreen");Mc.setScreen((net.minecraft.client.gui.screens.Screen)type.getMethod("createScreen",net.minecraft.client.gui.screens.Screen.class).invoke(null,(Object)null));}
    catch(ClassNotFoundException absent){Mc.setScreen(new net.minecraft.client.gui.screens.options.VideoSettingsScreen(null,mc,mc.options));}
  }
  private void finish(Throwable failure){
    try {
      if(failure!=null){failure.printStackTrace();JsonObject item=new JsonObject();item.addProperty("name",failure.toString());item.addProperty("passed",false);checks.add(item);}
      JsonObject report=new JsonObject();report.addProperty("version",net.fabricmc.loader.api.FabricLoader.getInstance().getModContainer("tflcore").orElseThrow().getMetadata().getVersion().getFriendlyString());report.addProperty("scope","UI only; no world, shaders or map certification");report.add("checks",checks);
      Files.writeString(out.resolve("visual-report.json"),new GsonBuilder().setPrettyPrinting().create().toJson(report));
    }catch(Exception e){throw new RuntimeException(e);}finally{phase=999;wait=Integer.MAX_VALUE;mc.stop();}
  }
}

*///?}
