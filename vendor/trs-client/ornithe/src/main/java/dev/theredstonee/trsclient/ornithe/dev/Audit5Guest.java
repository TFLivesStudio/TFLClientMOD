package dev.theredstonee.trsclient.ornithe.dev;

import com.google.gson.*;
import java.nio.file.*;
import net.fabricmc.loader.api.FabricLoader;
import net.ornithemc.osl.entrypoints.api.ModInitializer;
import net.ornithemc.osl.lifecycle.api.client.MinecraftClientEvents;

/** Separate native guest fixture. Its artifact has no Core dependency or Core classes. */
public final class Audit5Guest implements ModInitializer {
    private final long started=System.nanoTime();
    private long joined;
    private boolean ready,finished;
    public void init(){MinecraftClientEvents.TICK_END.register(mc->{
        if(finished)return;
        try{
            if(mc.options!=null){mc.options.pauseOnUnfocus=false;mc.options.viewDistance=2;mc.options.fpsLimit=30;mc.options.vsync=false;}
            if(FabricLoader.getInstance().isModLoaded("tflcore"))throw new AssertionError("Guest must not have Core installed");
            if(mc.world!=null && mc.player!=null){
                if(joined==0)joined=System.nanoTime();
                if(!ready && System.nanoTime()-joined>3_000_000_000L){
                    ready=true;JsonObject result=new JsonObject();result.addProperty("joined",true);result.addProperty("coreInstalled",false);result.addProperty("player",mc.player.getName());
                    Files.writeString(mc.gameDir.toPath().resolve("guest-ready.json"),new Gson().toJson(result));
                }
            }else if(ready){
                JsonObject result=new JsonObject();result.addProperty("status","passed");result.addProperty("coreInstalled",false);result.addProperty("joinedRealMinecraftWorld",true);result.addProperty("disconnectedAfterHostStop",true);
                Files.writeString(mc.gameDir.toPath().resolve("guest-result.json"),new GsonBuilder().setPrettyPrinting().create().toJson(result));finished=true;mc.stop();
            }
            if(System.nanoTime()-started>180_000_000_000L)throw new AssertionError("Native guest join/stop timeout");
        }catch(Throwable failure){failure.printStackTrace();finished=true;try{JsonObject result=new JsonObject();result.addProperty("status","failed");result.addProperty("error",failure.toString());Files.writeString(mc.gameDir.toPath().resolve("guest-result.json"),new Gson().toJson(result));}catch(Exception e){e.printStackTrace();}mc.stop();}
    });}
}
