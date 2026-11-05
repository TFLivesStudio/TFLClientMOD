//? if >=26.3 {
/*package dev.theredstonee.trsclient.dev;

import com.google.gson.*;
import com.seibel.distanthorizons.common.wrappers.world.ClientLevelWrapper;
import com.seibel.distanthorizons.core.api.internal.SharedApi;
import com.seibel.distanthorizons.core.world.DhClientWorld;
import dev.theredstonee.trsclient.TrsClient;
import dev.theredstonee.trsclient.compat.dh.*;
import java.nio.file.*;
import java.util.*;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;

// Separate remote acceptance mod; never distributed inside Core.
public final class Audit5RemoteWorlds implements ClientModInitializer {
    private final JsonArray checks=new JsonArray();private final Set<Path> folders=new HashSet<>();
    private final long started=System.nanoTime();private boolean finished;private int phase,wait,worlds;
    private String firstId,firstNamespace;private Path firstFolder;
    private DhWorldSessions.TflLevel previous;
    public void onInitializeClient(){ClientTickEvents.END_CLIENT_TICK.register(this::tick);}
    private void check(String name,boolean passed){var check=new JsonObject();check.addProperty("name",name);check.addProperty("passed",passed);checks.add(check);System.out.println("[TFL Remote DH] "+name+": "+passed);if(!passed)throw new AssertionError(name);}
    private void tick(net.minecraft.client.Minecraft mc){if(finished||wait-->0)return;try{
        if(System.nanoTime()-started>180_000_000_000L)throw new AssertionError("Remote DH timeout "+phase+" world "+worlds);
        if(mc.level==null||mc.player==null)return;
        mc.options.pauseOnLostFocus=false;mc.options.renderDistance().set(2);TrsClient.get().modules().dynamicFps.setEnabled(false);TrsClient.get().modules().minimap.setEnabled(false);
        if(dev.diorama.WorldKey.current()==null)return;
        DhRuntimePolicy.tick(mc);
        if(!(SharedApi.getAbstractDhWorld() instanceof DhClientWorld remote))return;
        var wrapper=ClientLevelWrapper.getWrapper(mc.level);
        switch(phase){
            case 0 -> {
                check("Remote received level uses a catalog-scoped DH wrapper",wrapper instanceof DhWorldSessions.TflLevel);
                var scoped=(DhWorldSessions.TflLevel)wrapper;
                check("Diorama and DH share the same remote world identity",scoped.namespace.equals(dev.diorama.WorldKey.current()));
                firstId=dev.diorama.WorldKey.catalog().activeId();firstNamespace=scoped.namespace;phase=1;wait=30;
            }
            case 1 -> {
                var scoped=(DhWorldSessions.TflLevel)wrapper;remote.getOrLoadLevel(scoped);
                Path folder=remote.saveStructure.getSaveFolder(scoped).toPath().toAbsolutePath().normalize();
                if(!Files.isRegularFile(folder.resolve("DistantHorizons.sqlite")))return;
                check("Remote cache is isolated beneath the dedicated TFL Horizons root",folder.startsWith(mc.gameDirectory.toPath().resolve("tfl-horizons").toAbsolutePath().normalize()));
                check("Same-dimension catalog world has a distinct persistent DH folder #"+worlds,folders.add(folder));
                if(worlds==0)firstFolder=folder;
                previous=scoped;worlds++;
                if(worlds<10){dev.diorama.WorldKey.catalog().create();dev.diorama.WorldKey.selectedWorld();phase=2;wait=30;}
                else{dev.diorama.WorldKey.catalog().select(firstId);dev.diorama.WorldKey.selectedWorld();phase=3;wait=30;}
            }
            case 2 -> {
                check("Manual selection replaces wrapper without replacing Minecraft's dimension",wrapper!=previous && !((DhWorldSessions.TflLevel)wrapper).namespace.equals(previous.namespace));
                check("Wrapper reuse path also respects the current catalog world",ClientLevelWrapper.getWrapperIfDifferent(previous,mc.level)==wrapper);
                phase=1;
            }
            case 3 -> {
                var scoped=(DhWorldSessions.TflLevel)wrapper;
                check("Revisiting a catalog world restores its original namespace",scoped.namespace.equals(firstNamespace));
                check("Revisiting a catalog world reuses its original DH database",remote.saveStructure.getSaveFolder(scoped).toPath().toAbsolutePath().normalize().equals(firstFolder));
                check("All ten legitimate cache databases remain on disk",folders.size()==10&&folders.stream().allMatch(p->Files.isRegularFile(p.resolve("DistantHorizons.sqlite"))));
                for(var level:remote.getAllLoadedLevels())check("No loaded DH level belongs to another catalog world",!(level.getLevelWrapper() instanceof DhWorldSessions.TflLevel tfl)||tfl.namespace.equals(firstNamespace));
                finish(mc,null);
            }
        }
    }catch(Throwable failure){finish(mc,failure);}}
    private void finish(net.minecraft.client.Minecraft mc,Throwable failure){finished=true;var report=new JsonObject();report.addProperty("status",failure==null?"passed":"failed");report.add("checks",checks);report.addProperty("sameDimensionWorlds",worlds);report.addProperty("receivedRemoteMinecraftLevel",mc.level!=null);if(failure!=null){failure.printStackTrace();report.addProperty("error",failure.toString());}
        try{Files.writeString(mc.gameDirectory.toPath().resolve("remote-result.json"),new GsonBuilder().setPrettyPrinting().create().toJson(report));}catch(Exception e){e.printStackTrace();}mc.stop();}
}

*///?}
