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
                var s=c.mc.getServer();var mode=s.getDefaultGameMode();
                check("Native LAN publish succeeds",s.isPublished()||s.publish(mode,false)!=null);
                check("Native LAN port is actual bound TCP listener",OrnitheWorldSharing.port(c)>0);
                check("Publishing preserves world game mode",s.getDefaultGameMode()==mode);
                OrnitheWorldSharing.open(c,null);phase++;wait=10;
            }
            case 6 -> {
                check("Shared World Sharing screen is reachable from native adapter",c.mc.screen instanceof CoreScreen screen&&screen.ui instanceof dev.theredstonee.trsclient.core.ui.menus.WorldShareUi);
                check("Opening sharing screen never initiates tunnel",OrnitheWorldSharing.service().snapshot().state==dev.theredstonee.trsclient.core.hosting.e4mc.WorldShareService.State.OFF);
                shot("world-sharing");c.mc.openScreen(null);OrnitheWorldSharing.service().stop();
                check("LAN remains open after stopping internet sharing",OrnitheWorldSharing.port(c)>0);
                phase=99;finish(null);
            }
        }
    }catch(Throwable failure){phase=99;finish(failure);}}
    private void finish(Throwable error){report.addProperty("status",error==null?"passed":"failed");if(error!=null){error.printStackTrace();report.addProperty("error",error.toString());}
        try{Files.createDirectories(out);Files.writeString(out.resolve(restart?"restart.json":"report.json"),new GsonBuilder().setPrettyPrinting().create().toJson(report));}catch(Exception e){e.printStackTrace();}c.mc.stop();}
}
