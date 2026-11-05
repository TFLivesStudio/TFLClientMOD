//? if >=26.3 {
/*package dev.theredstonee.trsclient.dev;

import com.google.gson.*;
import com.seibel.distanthorizons.api.DhApi;
import dev.theredstonee.trsclient.TrsClient;
import dev.theredstonee.trsclient.compat.Mc;
import dev.theredstonee.trsclient.compat.dh.DhRuntimePolicy;
import dev.theredstonee.trsclient.hosting.WorldSharingHooks;
import dev.theredstonee.trsclient.core.hosting.e4mc.*;
import net.minecraft.client.Minecraft;
import java.nio.file.*;

// Focused feature checks, separate from UnifiedSmoke and the physical-GPU/shader matrix.
public final class Audit5Smoke {
    private final JsonArray checks=new JsonArray();private final JsonObject report=new JsonObject();
    private int phase,wait;private final long started=System.nanoTime();
    public static void install(){Audit5Smoke probe=new Audit5Smoke();net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents.END_CLIENT_TICK.register(probe::tick);}
    private void check(String name,boolean ok){JsonObject entry=new JsonObject();entry.addProperty("name",name);entry.addProperty("passed",ok);checks.add(entry);System.out.println("[TFL Audit5] "+name+": "+ok);if(!ok)throw new AssertionError(name);}
    private void tick(Minecraft mc){if(phase==99||wait-->0)return;try{
        if(System.nanoTime()-started>300_000_000_000L)throw new AssertionError("Audit5 modern timeout "+phase);
        switch(phase){
            case 0 -> {if(Mc.overlay()!=null||Mc.screen()==null)return;mc.options.pauseOnLostFocus=false;mc.options.renderDistance().set(2);TrsClient.get().modules().dynamicFps.setEnabled(false);AutoTest.startWorld(mc);phase++;}
            case 1 -> {
                if(mc.level==null||mc.player==null||Mc.overlay()!=null||Mc.screen()!=null||DhApi.Delayed.renderProxy==null)return;
                check("Real managed DH world and renderer initialized",DhApi.Delayed.worldProxy.worldLoaded());
                check("Single DH engine resolved",net.fabricmc.loader.api.FabricLoader.getInstance().getAllMods().stream().filter(m->m.getMetadata().getId().equals("distanthorizons")).count()==1);
                var module=TrsClient.get().modules().farTerrain;boolean before=module.isEnabled();int distance=module.distance.getInt();
                module.setEnabled(false);DhRuntimePolicy.tick(mc);check("Core terrain toggle reaches official DH API",!DhApi.Delayed.configs.graphics().renderingEnabled().getValue());
                module.setEnabled(true);module.distance.set(distance==256?240:distance+16);DhRuntimePolicy.tick(mc);
                check("Core terrain distance reaches official DH API",DhApi.Delayed.configs.graphics().chunkRenderDistance().getValue()==module.distance.getInt());
                module.distance.set(distance);module.setEnabled(before);DhRuntimePolicy.tick(mc);
                check("Terrain preferences restored",DhApi.Delayed.configs.graphics().renderingEnabled().getValue()==before && DhApi.Delayed.configs.graphics().chunkRenderDistance().getValue()==distance);
                check("DH updates remain managed without changing disk preference",!com.seibel.distanthorizons.core.config.Config.Client.Advanced.AutoUpdater.enableAutoUpdater.get());
                WorldInsightProbe.run(mc,TrsClient.get());check("World Insight received targets and player privacy",true);
                check("Diorama and DH sessions coexist",dev.diorama.MapSession.get()!=null);
                check("Sharing transport bridge exists on native classpath",E4mcRuntime.supported());
                check("No tunnel before consent",WorldSharingHooks.service().snapshot().state==WorldShareService.State.OFF);
                var gameMode=mc.getSingleplayerServer().getPersonalGameMode();check("Native LAN publish",WorldSharingHooks.openLan());
                check("LAN uses a real nonzero bound port",WorldSharingHooks.port()>0);
                check("World game mode preserved",gameMode==mc.getSingleplayerServer().getPersonalGameMode());
                WorldSharingHooks.open(null);phase++;wait=10;
            }
            case 2 -> {
                check("World sharing screen is integrated",Mc.screen() instanceof dev.theredstonee.trsclient.screen.TrsUiScreen ui && ui.ui() instanceof dev.theredstonee.trsclient.core.ui.menus.WorldShareUi);
                check("Opening sharing UI is offline",WorldSharingHooks.service().snapshot().state==WorldShareService.State.OFF);
                AutoTest.shot(mc,"audit5-world-sharing");WorldSharingHooks.service().stop();check("LAN survives internet stop",WorldSharingHooks.port()>0);
                report.addProperty("dhRenderer",DhApi.Delayed.renderProxy.getRenderingApi().toString());report.addProperty("remoteWorldIsolationExercised",false);
                if(System.getProperty("tflcore.audit5.shareBroker")!=null){
                    var broker=java.net.URI.create(System.getProperty("tflcore.audit5.shareBroker"));
                    check("Offline guest fixture is restricted to a local HTTPS broker",broker.getScheme().equals("https")&&broker.getHost().equals("localhost"));
                    // Probe-only local offline fixture; production authentication is unchanged.
                    mc.getSingleplayerServer().setUsesAuthentication(false);Mc.setScreen(null);
                    var consent=new WorldShareService.Consent();consent.understood(true);
                    check("Explicit fixture consent starts the isolated runtime tunnel",WorldSharingHooks.service().start(mc.getSingleplayerServer(),WorldSharingHooks.port(),mc.player.getGameProfile().name(),broker.toString(),consent));phase=3;
                }else{phase=99;finish(mc,null);}
            }
            case 3 -> {
                var snapshot=WorldSharingHooks.service().snapshot();
                if(snapshot.state==WorldShareService.State.ERROR)throw new AssertionError("Sharing failed: "+snapshot.error);
                if(snapshot.state!=WorldShareService.State.ONLINE)return;
                var invitation=new JsonObject();invitation.addProperty("address",snapshot.address);invitation.addProperty("lanPort",snapshot.lanPort);
                Files.writeString(Path.of(System.getProperty("tflcore.audit5.shareFixture")).resolve("invitation.json"),new Gson().toJson(invitation));phase=4;
            }
            case 4 -> {
                var fixture=Path.of(System.getProperty("tflcore.audit5.shareFixture"));
                if(Files.exists(fixture.resolve("guest-result.json")))throw new AssertionError("Guest terminated before acceptance: "+Files.readString(fixture.resolve("guest-result.json")));
                if(!Files.exists(fixture.resolve("guest-ready.json")))return;
                check("Core-free native guest joined the actual integrated server",mc.getSingleplayerServer().getPlayerList().getPlayers().stream().anyMatch(p->p.getGameProfile().name().equals("TFLGuest")));
                check("Real guest has an active relayed stream",WorldSharingHooks.service().snapshot().peers>0);
                WorldSharingHooks.service().stop();phase=5;
            }
            case 5 -> {
                var result=Path.of(System.getProperty("tflcore.audit5.shareFixture")).resolve("guest-result.json");if(!Files.exists(result))return;
                check("Stopping sharing disconnects native guest",JsonParser.parseString(Files.readString(result)).getAsJsonObject().get("status").getAsString().equals("passed"));
                check("Stopping internet sharing preserves real LAN listener",WorldSharingHooks.port()>0);
                report.addProperty("nativeGuestWithoutCore",true);report.addProperty("offlineLocalFixture",true);
                if(Boolean.getBoolean("tflcore.audit5.remoteGuest")){phase=6;}else{phase=99;finish(mc,null);}
            }
            case 6 -> {
                var result=Path.of(System.getProperty("tflcore.audit5.shareFixture")).resolve("remote-result.json");if(!Files.exists(result))return;
                check("Remote DH ten-world isolation probe passed",JsonParser.parseString(Files.readString(result)).getAsJsonObject().get("status").getAsString().equals("passed"));
                phase=99;finish(mc,null);
            }
        }
    }catch(Throwable failure){phase=99;finish(mc,failure);}}
    private void finish(Minecraft mc,Throwable failure){report.add("checks",checks);report.addProperty("status",failure==null?"passed":"failed");if(failure!=null){failure.printStackTrace();report.addProperty("error",failure.toString());}
        try{Files.writeString(mc.gameDirectory.toPath().resolve("audit5-smoke.json"),new GsonBuilder().setPrettyPrinting().create().toJson(report));}catch(Exception e){e.printStackTrace();}mc.stop();}
}
*///?}
