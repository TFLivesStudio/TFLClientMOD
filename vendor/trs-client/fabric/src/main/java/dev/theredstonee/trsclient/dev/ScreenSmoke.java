package dev.theredstonee.trsclient.dev;

import com.google.gson.GsonBuilder;
import dev.theredstonee.trsclient.compat.Mc;
import dev.theredstonee.trsclient.screen.TrsTitleScreen;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import java.nio.file.Files;
import java.nio.charset.StandardCharsets;
import java.util.*;

/** Opt-in native navigation through original Minecraft callbacks, without changing options. */
public final class ScreenSmoke {
    private final String[] keys={"options.video","options.sounds","options.controls","options.controls/controls.keybinds","options.controls/options.mouse_settings","options.language","options.resourcepack","options.accessibility","options.chat","options.skinCustomisation"};
    private final List<Map<String,Object>> results=new ArrayList<>();
    private int phase,index,wait,titleWarmup; private Screen options; private String target; private Screen previous;
    private final long start=System.currentTimeMillis();
    public static void install(){ScreenSmoke t=new ScreenSmoke();net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents.END_CLIENT_TICK.register(t::tick);}

  private int videoProbeStage;
  private long videoProbeAt;
  private final com.google.gson.JsonArray videoEvidence=new com.google.gson.JsonArray();
  private boolean videoProbe(){
    var bg=dev.theredstonee.trsclient.core.ui.background.MenuBackground.shared();
    var m=bg.metrics();long now=System.nanoTime();
    if(videoProbeAt==0){if(m.framesPresented<2)return false;videoProbeAt=now;AutoTest.shot(Minecraft.getInstance(),"depth-video-start");return false;}
    double elapsed=(now-videoProbeAt)/1e9;
    double duration=videoProbeStage==0?20:videoProbeStage==1||videoProbeStage==2||videoProbeStage==5?3:1;
    if(elapsed<duration||videoProbeStage==0&&m.loops<1&&elapsed<45)return false;
    var item=new com.google.gson.JsonObject();item.addProperty("scenario",videoProbeStage);item.add("metrics",new com.google.gson.Gson().toJsonTree(m));videoEvidence.add(item);
    AutoTest.shot(Minecraft.getInstance(),"depth-video-"+videoProbeStage);
    if(videoProbeStage==0){if(m.loops<1||m.framesPresented<20)throw new AssertionError("Native video loop/presentation did not advance");dev.theredstonee.trsclient.TrsClient.get().modules().menuBackground.set(dev.theredstonee.trsclient.core.ui.background.MenuBackground.Mode.HIGH);}
    else if(videoProbeStage==1){if(m.framesPresented<3)throw new AssertionError("Quality video did not advance");dev.theredstonee.trsclient.TrsClient.get().modules().menuBackground.set(dev.theredstonee.trsclient.core.ui.background.MenuBackground.Mode.PERFORMANCE);}
    else if(videoProbeStage==2){if(m.framesPresented<3)throw new AssertionError("Performance video did not advance");dev.theredstonee.trsclient.TrsClient.get().modules().menuBackground.set(dev.theredstonee.trsclient.core.ui.background.MenuBackground.Mode.STATIC);}
    else if(videoProbeStage==3){if(bg.videoActive())throw new AssertionError("Static preference started a decoder");dev.theredstonee.trsclient.TrsClient.get().modules().menuBackground.set(dev.theredstonee.trsclient.core.ui.background.MenuBackground.Mode.OFF);}
    else if(videoProbeStage==4){if(bg.videoActive())throw new AssertionError("Off preference started a decoder");bg.close();dev.theredstonee.trsclient.TrsClient.get().modules().menuBackground.set(dev.theredstonee.trsclient.core.ui.background.MenuBackground.Mode.BALANCED);}
    else {if(m.framesPresented<3)throw new AssertionError("Video did not restart");return true;}
    videoProbeStage++;videoProbeAt=now;return false;
  }
    private void tick(Minecraft mc){
        if(wait-->0)return;
        try {
            if(System.currentTimeMillis()-start>240000)throw new IllegalStateException("navigation timeout");
            if(Mc.overlay()!=null)return;
            if(phase==0){if(Mc.screen()==null)return;if(titleWarmup++<30)return;if(!videoProbe())return;record("bundledVideoPresented",dev.theredstonee.trsclient.core.ui.background.MenuBackground.shared().videoFramePresented(),"decoded bundled blue video reached the native renderer");AutoTest.shot(mc,"glass-title");options=MenusTest.options(new TrsTitleScreen(),mc);Mc.setScreen(options);phase=1;wait=20;return;}
            if(phase==1){
                if(index>=keys.length){finish(mc);phase=3;return;}
                target=keys[index++];previous=options;
                if(!press(options,target.split("/")[0]))return;
                phase=target.contains("/")?4:2;wait=18;return;
            }
            if(phase==4){previous=Mc.screen();if(!press(previous,target.split("/")[1])){Mc.setScreen(options);phase=1;return;}phase=2;wait=18;return;}

            if(phase==2){
                Screen s=Mc.screen();boolean opened=s!=null&&s!=previous;
                record(target,opened,s==null?"null":s.getClass().getName());
                if(target.endsWith("controls.keybinds"))checkBindings(mc);
                AutoTest.shot(mc,"tfl-screen-"+target.replace('.','-').replace('/','-'));
                Mc.setScreen(options);phase=1;wait=10;
            }
        }catch(Throwable e){record("exception",false,e.toString());finish(mc);phase=3;}
    }
    private void checkBindings(Minecraft mc) throws Exception {
        Class<?> helper=dev.theredstonee.trsclient.menus.KeySearchUi.class;
        java.lang.reflect.Field listField=helper.getDeclaredField("list");listField.setAccessible(true);
        Object list=listField.get(null);
        java.lang.reflect.Method keyOf=helper.getDeclaredMethod("keyOf",Object.class);keyOf.setAccessible(true);
        boolean clean=list!=null;
        if(list instanceof net.minecraft.client.gui.components.AbstractSelectionList) {
            for(Object row:((net.minecraft.client.gui.components.AbstractSelectionList<?>)list).children()) {
                net.minecraft.client.KeyMapping key=(net.minecraft.client.KeyMapping)keyOf.invoke(null,row);
                if(key!=null&&!dev.theredstonee.trsclient.core.keys.KeyVisibility.visible(key.getName()))clean=false;
            }
        }
        record("offlineBindingsHidden",clean,"visible key-list rows only; registered mappings preserved");
        java.util.Set<String> registered=new java.util.HashSet<>();
        for(net.minecraft.client.KeyMapping key:mc.options.keyMappings)registered.add(key.getName());
        record("offlineBindingsPreserved",registered.containsAll(java.util.Arrays.asList("key.trsclient.wardrobe","key.trsclient.social","key.trsclient.quickReply","key.trsclient.saveClip","key.trsclient.toggleRecording")),"all five mappings remain registered for preference persistence");
    }
    private boolean press(Screen screen,String key) {
        String label=Component.translatable(key).getString();Button found=screen==null?null:find(screen,label,0);
        if(found==null){record(target,false,"button not found: "+label);return false;}
        //? if >=1.21.9 {
        /*found.onPress(new net.minecraft.client.input.KeyEvent(257,0,0));
        *///?} else
        found.onPress();
        return true;
    }
    private Button find(net.minecraft.client.gui.components.events.ContainerEventHandler container,String label,int depth) {
        if(depth>8)return null;
        for(Object child:container.children()) {
            if(child instanceof Button&&((Button)child).getMessage().getString().equals(label))return (Button)child;
            if(child instanceof net.minecraft.client.gui.components.events.ContainerEventHandler) {
                Button found=find((net.minecraft.client.gui.components.events.ContainerEventHandler)child,label,depth+1);
                if(found!=null)return found;
            }
        }
        return null;
    }
    private void record(String key,boolean passed,String detail){Map<String,Object> r=new LinkedHashMap<>();r.put("destination",key);r.put("passed",passed);r.put("detail",detail);results.add(r);}
    private void finish(Minecraft mc){
        Map<String,Object> out=new LinkedHashMap<>();out.put("scope","native option navigation and rendered frames; no exhaustive interaction certification");out.put("elapsedMs",System.currentTimeMillis()-start);out.put("results",results);out.put("videoEvidence",videoEvidence);
        try{Files.write(mc.gameDirectory.toPath().resolve("screen-smoke.json"),new GsonBuilder().setPrettyPrinting().create().toJson(out).getBytes(StandardCharsets.UTF_8));}catch(Exception e){throw new RuntimeException(e);}
        mc.stop();
    }
}
