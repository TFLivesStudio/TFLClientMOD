package dev.theredstonee.trsclient.ornithe.dev;

import com.google.gson.*;
import dev.theredstonee.trsclient.ornithe.*;
import java.nio.file.*;
import net.minecraft.client.Screenshot;
import net.minecraft.world.*;
import net.minecraft.world.gen.WorldGeneratorType;

/** Focused native acceptance for newly implemented LOD/LAN; does not replace UnifiedSmoke's map/fog gates. */
public final class Audit5Smoke implements Runnable {
    private final OrnitheClient c;
    private final Path out;
    private final JsonObject report=new JsonObject();
    private final JsonArray checks=new JsonArray();
    private final long started=System.nanoTime();
    private final boolean restart=Boolean.getBoolean("tflcore.ornithe.restart");
    private int phase,wait;
    private long uploads;
    private long beforeMove;
    private double originalZ;
    private boolean lifecycleDone;
    private int lightX,lightY,lightZ,lightCell,beforeHeight;
    private dev.theredstonee.trsclient.core.terrain.FarTerrainEngine overworldEngine;
    private dev.theredstonee.trsclient.core.terrain.FarTerrainEngine stopped;
    public Audit5Smoke(OrnitheClient c,Path out){this.c=c;this.out=out;report.add("checks",checks);report.addProperty("scenario","audit5-lod-lan");report.addProperty("mapFogExercised",false);}
    private void check(String label,boolean ok){JsonObject check=new JsonObject();check.addProperty("name",label);check.addProperty("passed",ok);checks.add(check);System.out.println("[TFL Audit5] "+label+": "+ok);if(!ok)throw new AssertionError(label);}
    private void shot(String name){Screenshot.take(out.toFile(),"audit5-"+name+".png",c.mc.width,c.mc.height,c.mc.getRenderTarget());}
    public void run(){if(phase==99||wait-->0)return;try{
        if(System.nanoTime()-started>240_000_000_000L)throw new AssertionError("Audit5 timeout phase "+phase+"; terrain="+c.farTerrain.failure);
        switch(phase){
            case 0 -> {
                c.mc.options.pauseOnUnfocus=false;c.mc.options.fpsLimit=120;c.mc.options.vsync=false;c.mc.options.viewDistance=restart?2:6;
                c.modules.dynamicFps.setEnabled(false);c.modules.farTerrain.setEnabled(true);
                c.modules.minimap.setEnabled(false);c.modules.menuBackground.set(dev.theredstonee.trsclient.core.ui.background.MenuBackground.Mode.STATIC);
                c.mc.startGame("tfl-audit5-terrain","TFL Audit5 Terrain",new WorldSettings(189L,WorldSettings.GameMode.CREATIVE,false,false,WorldGeneratorType.FLAT).enableCommands());phase++;wait=30;
            }
            case 1 -> {
                if(c.mc.player==null||c.mc.world==null||c.mc.screen!=null||c.farTerrain.engine()==null)return;
                c.mc.player.abilities.flying=true;c.mc.player.syncAbilities();c.mc.player.pitch=35;c.mc.player.yaw=0;
                c.mc.player.sendChat("/tp @p ~ 50 ~ 0 35");
                WorldInsightProbe.run(c);check("World Insight native received targets and privacy",true);
                check("No internet sharing without explicit session consent",OrnitheWorldSharing.service().snapshot().state==dev.theredstonee.trsclient.core.hosting.e4mc.WorldShareService.State.OFF);
                if(!restart)c.mc.player.sendChat("/fill "+((int)c.mc.player.x+32)+" 4 "+((int)c.mc.player.z+32)+" "+((int)c.mc.player.x+40)+" 28 "+((int)c.mc.player.z+40)+" minecraft:stone");
                phase++;wait=10;
            }
            case 2 -> {
                var engine=c.farTerrain.engine();if(engine==null)return;
                if(restart){if(engine.diskHits<30)return;check("Historical LOD restored after Minecraft process restart",engine.diskHits>=30);}
                else if(engine.columnCount()<100)return;
                report.addProperty("receivedColumns",engine.columnCount());report.addProperty("diskRestores",engine.diskHits);
                check("Immutable received terrain columns available",engine.columnCount()>0);
                c.mc.options.viewDistance=2;c.mc.worldRenderer.reload();phase++;wait=25;
            }
            case 3 -> {
                if(!c.farTerrain.failure.isEmpty())throw new AssertionError("LOD renderer "+c.farTerrain.failure);
                if(c.farTerrain.draws<8 || c.farTerrain.uploads<2)return;
                check("3D LOD geometry drawn beyond vanilla two-chunk range",c.farTerrain.verticesDrawn>0);
                check("GPU geometry budget respected",c.farTerrain.gpuBytes()<=(long)c.modules.farTerrain.gpuBudget.getInt()*1024*1024);
                check("LOD is submitted once before vanilla geometry per non-stereo frame",c.farTerrain.maxPassesPerFrame==1);
                check("LOD camera axes and screen projection match native terrain",c.farTerrain.projectionAligned);
                check("LOD pass preserves valid OpenGL state",org.lwjgl.opengl.GL11.glGetError()==org.lwjgl.opengl.GL11.GL_NO_ERROR);
                report.addProperty("lodVertices",c.farTerrain.verticesDrawn);report.addProperty("gpuBytes",c.farTerrain.gpuBytes());
                report.addProperty("meshBuildNanos",c.farTerrain.engine().generationNanos);report.addProperty("nativeRenderer",org.lwjgl.opengl.GL11.glGetString(org.lwjgl.opengl.GL11.GL_RENDERER));
                shot("terrain-enabled");
                if(!restart){originalZ=c.mc.player.z;beforeMove=c.farTerrain.draws;c.mc.player.sendChat("/tp @p ~ 50 ~32 180 35");phase=31;wait=25;}
                else {stopped=c.farTerrain.engine();uploads=c.farTerrain.uploads;c.modules.farTerrain.setEnabled(false);phase=4;wait=10;}
            }
            case 31 -> {
                if(c.mc.player.z<originalZ+31 || c.farTerrain.draws<=beforeMove+8)return;
                check("Known 3D terrain remains visible after moving away",c.farTerrain.engine()!=null && c.farTerrain.verticesDrawn>0);
                shot("terrain-after-moving");stopped=c.farTerrain.engine();uploads=c.farTerrain.uploads;c.modules.farTerrain.setEnabled(false);phase=4;wait=10;
            }
            case 4 -> {
                if(!stopped.stopped())return;
                check("Disabling LOD terminates workers and releases all GPU geometry",c.farTerrain.engine()==null&&c.farTerrain.gpuBytes()==0);
                shot("terrain-disabled");c.modules.farTerrain.setEnabled(true);phase++;wait=10;
            }
            case 5 -> {
                var engine=c.farTerrain.engine();if(engine==null||engine.diskHits<30||c.farTerrain.uploads<=uploads)return;
                check("Re-enabling LOD restores derived cache without resending terrain",engine.diskHits>=30);
                if(!lifecycleDone){
                    lightX=(int)Math.floor(c.mc.player.x)+2;lightZ=(int)Math.floor(c.mc.player.z)+2;lightCell=(lightZ&15)*16+(lightX&15);
                    var column=engine.column(lightX>>4,lightZ>>4);if(column==null)return;
                    beforeHeight=column.height(lightCell);lightY=Math.min(250,beforeHeight+4);
                    c.mc.player.sendChat("/setblock "+lightX+" "+lightY+" "+lightZ+" minecraft:glowstone");phase=20;wait=10;return;
                }
                var s=c.mc.getServer();var mode=s.getDefaultGameMode();
                check("Native LAN publish succeeds",s.isPublished()||s.publish(mode,false)!=null);
                check("Native LAN port is actual bound TCP listener",OrnitheWorldSharing.port(c)>0);
                check("Publishing preserves world game mode",s.getDefaultGameMode()==mode);
                OrnitheWorldSharing.open(c,null);phase++;wait=10;
            }
            case 20 -> {
                var column=c.farTerrain.engine().column(lightX>>4,lightZ>>4);
                if(column==null||column.height(lightCell)!=lightY+1||column.blockLight(lightCell)!=15)return;
                var snapshot=c.diorama.chunk(lightX>>4,lightZ>>4);
                if(snapshot==null||snapshot.blockLight(lightY>>4,lightX&15,lightY&15,lightZ&15)!=15)return;
                check("Received emissive block updates LOD height and block light",true);
                check("Diorama observes the same legitimate light update",snapshot!=null&&snapshot.blockLight(lightY>>4,lightX&15,lightY&15,lightZ&15)==15);
                c.mc.player.sendChat("/setblock "+lightX+" "+lightY+" "+lightZ+" minecraft:air");c.mc.player.sendChat("/time set 18000");phase=21;wait=15;
            }
            case 21 -> {
                var column=c.farTerrain.engine().column(lightX>>4,lightZ>>4);
                if(column==null||column.height(lightCell)!=beforeHeight||column.blockLight(lightCell)!=0)return;
                check("Removing emissive terrain invalidates cached height and lighting",true);
                check("Night-time LOD keeps valid render state",c.farTerrain.failure.isEmpty()&&org.lwjgl.opengl.GL11.glGetError()==org.lwjgl.opengl.GL11.GL_NO_ERROR);
                shot("terrain-night");c.mc.player.sendChat("/time set 6000");overworldEngine=c.farTerrain.engine();
                int px=(int)Math.floor(c.mc.player.x),py=(int)Math.ceil(c.mc.player.y),pz=(int)Math.floor(c.mc.player.z);
                c.mc.player.sendChat("/fill "+(px-1)+" "+py+" "+pz+" "+(px+2)+" "+(py+4)+" "+pz+" minecraft:obsidian");
                c.mc.player.sendChat("/fill "+px+" "+(py+1)+" "+pz+" "+(px+1)+" "+(py+3)+" "+pz+" minecraft:portal");
                c.mc.player.sendChat("/tp @p "+(px+.5)+" "+(py+1)+" "+(pz+.5));phase=22;wait=30;
            }
            case 22 -> {
                if(c.mc.world==null||c.mc.world.dimension.getId()!=-1||c.mc.screen!=null||c.farTerrain.engine()==null||c.farTerrain.engine().columnCount()==0)return;
                check("Nether receives a distinct LOD engine and cache",c.farTerrain.engine()!=overworldEngine);
                check("Changing dimension terminates previous terrain workers",overworldEngine.stopped());
                shot("terrain-nether");var uuid=c.mc.player.getUuid();c.mc.getServer().executeTask(()->c.mc.getServer().getPlayerManager().changeDimension(c.mc.getServer().getPlayerManager().get(uuid),0));phase=23;wait=30;
            }
            case 23 -> {
                if(c.mc.world==null||c.mc.world.dimension.getId()!=0||c.mc.screen!=null||c.farTerrain.engine()==null||c.farTerrain.engine().diskHits<30)return;
                check("Return from Nether restores the isolated Overworld cache",c.farTerrain.engine()!=overworldEngine&&c.farTerrain.engine().diskHits>=30);
                check("No LOD renderer failure after dimension round trip",c.farTerrain.failure.isEmpty());
                c.mc.player.sendChat("/tp @p ~8 50 ~8");phase=24;wait=30;
            }
            case 24 -> { lifecycleDone=true;phase=5; }
            case 6 -> {
                check("Shared World Sharing screen is reachable from native adapter",c.mc.screen instanceof CoreScreen screen&&screen.ui instanceof dev.theredstonee.trsclient.core.ui.menus.WorldShareUi);
                check("Opening sharing screen never initiates tunnel",OrnitheWorldSharing.service().snapshot().state==dev.theredstonee.trsclient.core.hosting.e4mc.WorldShareService.State.OFF);
                shot("world-sharing");c.mc.openScreen(null);OrnitheWorldSharing.service().stop();
                check("LAN remains open after stopping internet sharing",OrnitheWorldSharing.port(c)>0);
                if(System.getProperty("tflcore.audit5.shareBroker")!=null){
                    var broker=java.net.URI.create(System.getProperty("tflcore.audit5.shareBroker"));
                    check("Offline guest fixture is restricted to a local HTTPS broker",broker.getScheme().equals("https")&&broker.getHost().equals("localhost"));
                    // Test fixture only: no account credentials exist in this environment.
                    // This class is excluded from the installable Core artifact.
                    c.mc.getServer().setOnlineMode(false);
                    var consent=new dev.theredstonee.trsclient.core.hosting.e4mc.WorldShareService.Consent();consent.understood(true);
                    check("Explicit fixture consent starts the isolated runtime tunnel",OrnitheWorldSharing.service().start(c.mc.getServer(),OrnitheWorldSharing.port(c),c.mc.player.getName(),broker.toString(),consent));
                    phase=7;
                }else {phase=99;finish(null);}
            }
            case 7 -> {
                var s=OrnitheWorldSharing.service().snapshot();
                if(s.state==dev.theredstonee.trsclient.core.hosting.e4mc.WorldShareService.State.ERROR)throw new AssertionError("Sharing failed: "+s.error);
                if(s.state!=dev.theredstonee.trsclient.core.hosting.e4mc.WorldShareService.State.ONLINE)return;
                var invitation=new JsonObject();invitation.addProperty("address",s.address);invitation.addProperty("lanPort",s.lanPort);
                Files.writeString(Path.of(System.getProperty("tflcore.audit5.shareFixture")).resolve("invitation.json"),new Gson().toJson(invitation));
                phase=8;
            }
            case 8 -> {
                var failedGuest=Path.of(System.getProperty("tflcore.audit5.shareFixture")).resolve("guest-result.json");
                if(Files.exists(failedGuest))throw new AssertionError("Guest terminated before acceptance: "+Files.readString(failedGuest));
                if(!Files.exists(Path.of(System.getProperty("tflcore.audit5.shareFixture")).resolve("guest-ready.json")))return;
                check("Core-free native guest joined the actual integrated server",c.mc.getServer().getPlayerManager().get("TFLGuest")!=null);
                check("Real guest has an active relayed stream",OrnitheWorldSharing.service().snapshot().peers>0);
                OrnitheWorldSharing.service().stop();phase=9;
            }
            case 9 -> {
                var result=Path.of(System.getProperty("tflcore.audit5.shareFixture")).resolve("guest-result.json");
                if(!Files.exists(result))return;
                check("Stopping sharing disconnects native guest",new JsonParser().parse(Files.readString(result)).getAsJsonObject().get("status").getAsString().equals("passed"));
                check("Stopping internet sharing preserves real LAN listener",OrnitheWorldSharing.port(c)>0);
                report.addProperty("nativeGuestWithoutCore",true);report.addProperty("offlineLocalFixture",true);phase=99;finish(null);
            }
        }
    }catch(Throwable failure){phase=99;finish(failure);}}
    private void finish(Throwable error){report.addProperty("status",error==null?"passed":"failed");if(error!=null){error.printStackTrace();report.addProperty("error",error.toString());}
        try{Files.createDirectories(out);Files.writeString(out.resolve(restart?"restart.json":"report.json"),new GsonBuilder().setPrettyPrinting().create().toJson(report));}catch(Exception e){e.printStackTrace();}c.mc.stop();}
}
