package dev.theredstonee.trsclient.ornithe.dev;

import com.google.gson.*;
import dev.theredstonee.trsclient.core.ui.graphics.*;
import dev.theredstonee.trsclient.core.ui.menu.ModMenu;
import dev.theredstonee.trsclient.ornithe.*;
import java.nio.file.*;
import java.util.Objects;
import net.minecraft.client.Screenshot;

/** Focused UI-only probe; included only in the separate development probe JAR. */
public final class VisualSmoke implements Runnable {
  private final OrnitheClient c;
  private final Path out;
  private final JsonArray checks=new JsonArray();
  private final long started=System.nanoTime();
  private int phase,wait=60;
  private GraphicsProfiles profiles;
  private GraphicsProfiles.Plan plan;
  public VisualSmoke(OrnitheClient c,Path out){this.c=c;this.out=out;}
  private void check(String name,boolean pass){JsonObject item=new JsonObject();item.addProperty("name",name);item.addProperty("passed",pass);checks.add(item);if(!pass)throw new AssertionError(name);}
  private void shot(String name){Screenshot.take(out.toFile(),name+".png",c.mc.width,c.mc.height,c.mc.getRenderTarget());}
  public void run(){
    try {
      if(System.nanoTime()-started>100_000_000_000L)throw new AssertionError("UI probe timeout");
      if(wait-->0)return;
      System.out.println("[TFL Visual] phase "+phase);
      switch(phase++) {
        case 0 -> {shot("title");c.mc.options.guiScale=2;c.mc.openScreen(new CoreScreen(new ModMenu(new OrnitheMenuHost(c,null)),false));}
        case 1 -> {shot("modules");c.mc.openScreen(new CoreScreen(new ModMenu(new OrnitheMenuHost(c,null)).showSettingsHub(),false));}
        case 2 -> {shot("settings-hub");c.mc.openScreen(new CoreScreen(new ModMenu(new OrnitheMenuHost(c,null)).select(c.modules.keystrokes),false));}
        case 3 -> {shot("module-detail");openGraphics();}
        case 4 -> {
          check("graphics native host connected",c.mc.screen instanceof CoreScreen && ((CoreScreen)c.mc.screen).ui instanceof GraphicsUi);
          GraphicsUi ui=(GraphicsUi)((CoreScreen)c.mc.screen).ui;shot("profiles-before");
          profiles=new GraphicsProfiles(ui.provider());plan=profiles.prepare(GraphicsProfiles.Profile.BALANCED);
          check("native profile exposes supported changes",!plan.changes.isEmpty());profiles.apply(plan);
          for(var change:plan.changes)check("applied "+change.option.id(),Objects.equals(change.after,change.binding.read()));
        }
        case 5 -> {shot("profiles-applied");check("native undo available",profiles.canUndo());profiles.undo();for(var change:plan.changes)check("restored "+change.option.id(),Objects.equals(change.before,change.binding.read()));}
        case 6 -> {shot("profiles-restored");resize(1280,720);c.mc.openScreen(new CoreScreen(new ModMenu(new OrnitheMenuHost(c,null)),false));}
        case 7 -> {shot("modules-1280x720");resize(854,480);openGraphics();dev.theredstonee.trsclient.core.i18n.I18n.use("es");}
        case 8 -> {shot("profiles-854x480-es");c.mc.openScreen(new CoreScreen(new dev.theredstonee.trsclient.core.ui.menu.HudEditor(new OrnitheMenuHost(c,null)),false));}
        case 9 -> {shot("hud-library-854x480");finish(null);}
      }
      wait=25;
    }catch(Throwable failure){finish(failure);}
  }
  private void resize(int width,int height)throws Exception{
    org.lwjgl.opengl.Display.setDisplayMode(new org.lwjgl.opengl.DisplayMode(width,height));
    ((dev.theredstonee.trsclient.ornithe.mixin.MinecraftResizeAccess)c.mc).tfl$resize(width,height);
  }
  private void openGraphics()throws Exception {
    try {var type=Class.forName("dev.rdh.argentum.impl.gui.VideoOptionsScreen");c.mc.openScreen((net.minecraft.client.gui.screen.Screen)type.getConstructor(net.minecraft.client.gui.screen.Screen.class).newInstance((Object)null));}
    catch(ClassNotFoundException absent){c.mc.openScreen(new net.minecraft.client.gui.screen.menu.options.VideoOptionsScreen(null,c.mc.options));}
  }
  private void finish(Throwable failure){
    try {
      if(failure!=null){failure.printStackTrace();JsonObject item=new JsonObject();item.addProperty("name",failure.toString());item.addProperty("passed",false);checks.add(item);}
      JsonObject report=new JsonObject();report.addProperty("version",c.version());report.addProperty("scope","UI only; no world, shaders or map certification");report.add("checks",checks);
      Files.writeString(out.resolve("visual-report.json"),new GsonBuilder().setPrettyPrinting().create().toJson(report));
    }catch(Exception e){throw new RuntimeException(e);}finally{phase=999;wait=Integer.MAX_VALUE;c.mc.stop();}
  }
}
