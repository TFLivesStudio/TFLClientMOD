//? if >=26.3 {
/*package dev.theredstonee.trsclient.compat.dh;

import com.seibel.distanthorizons.api.DhApi;
import com.seibel.distanthorizons.api.interfaces.override.levelHandling.IDhApiSaveStructure;
import com.seibel.distanthorizons.api.interfaces.world.IDhApiLevelWrapper;
import com.seibel.distanthorizons.common.wrappers.level.ServerKeyedClientLevelWrapper;
import com.seibel.distanthorizons.common.wrappers.world.ClientLevelWrapper;
import com.seibel.distanthorizons.common.wrappers.chunk.ChunkWrapper;
import com.seibel.distanthorizons.core.api.internal.SharedApi;
import com.seibel.distanthorizons.core.world.DhClientWorld;
import com.seibel.distanthorizons.core.wrapperInterfaces.world.IClientLevelWrapper;
import dev.theredstonee.trsclient.mixin.DhSaveCacheAccess;
import java.io.File;
import java.lang.ref.WeakReference;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.*;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.world.level.chunk.status.ChunkStatus;

// DH 3.3.4 has a public save-path override, but no API for replacing a client level's
// identity after a manual subworld selection. A narrowly pinned wrapper-factory mixin
// supplies distinct DH identifiers; the original runtime and database format stay intact.
public final class DhWorldSessions {
    private static final Map<ClientLevel,Binding> BINDINGS=new WeakHashMap<>();
    private static final class Binding {
        String namespace="unresolved-"+UUID.randomUUID();
        WeakReference<TflLevel> wrapper=new WeakReference<>(null);
    }
    private static WeakReference<ClientLevel> replayLevel=new WeakReference<>(null);
    private static int cursor,radius,centerX,centerZ;
    private static boolean installed;
    private DhWorldSessions(){}
    public static void install(){
        DhApi.overrides.bind(IDhApiSaveStructure.class,new IDhApiSaveStructure(){
            public File overrideFilePath(File original,IDhApiLevelWrapper level){
                if(!(level instanceof TflLevel tfl))return null; // retain local-save/native caches unchanged
                return FabricLoader.getInstance().getGameDir().resolve("tfl-horizons").resolve(hash(tfl.namespace)).resolve(hash(tfl.getDimensionName())).toFile();
            }
        });installed=true;
    }
    // No catalog, network, disk or terrain reads here; DH may request a wrapper off the client thread.
    public static IClientLevelWrapper wrap(ClientLevel level,IClientLevelWrapper original){
        if(level==null||original==null)return original;
        synchronized(BINDINGS){
            Binding binding=BINDINGS.get(level);
            if(binding==null){
                if(Minecraft.getInstance().getSingleplayerServer()!=null)return original;
                binding=new Binding();BINDINGS.put(level,binding);
            }
            TflLevel current=binding.wrapper.get();
            if(current==null){current=new TflLevel(level,binding.namespace);binding.wrapper=new WeakReference<>(current);}
            return current;
        }
    }
    public static final class TflLevel extends ServerKeyedClientLevelWrapper {
        public final String namespace;
        TflLevel(ClientLevel level,String namespace){
            super(level,"tfl-"+hash(namespace),"tfl-"+hash(namespace+"/"+level.dimension().identifier()));this.namespace=namespace;
        }
    }
    public static void tick(Minecraft mc){
        if(!installed || mc.level==null || mc.getSingleplayerServer()!=null){replayLevel=new WeakReference<>(null);return;}
        String namespace=dev.diorama.WorldKey.current(mc.level);
        if(namespace==null)return; // quarantine namespace stays isolated while a server's ID is pending
        TflLevel old=null;
        synchronized(BINDINGS){
            Binding binding=BINDINGS.computeIfAbsent(mc.level,ignored->new Binding());
            if(!namespace.equals(binding.namespace)){
                old=binding.wrapper.get();binding.namespace=namespace;binding.wrapper=new WeakReference<>(null);
                replayLevel=new WeakReference<>(mc.level);cursor=0;radius=Math.min(64,mc.options.getEffectiveRenderDistance());
                centerX=mc.player==null?0:mc.player.chunkPosition().x();centerZ=mc.player==null?0:mc.player.chunkPosition().z();
            }
        }
        var world=SharedApi.getAbstractDhWorld();
        if(world instanceof DhClientWorld remote){
            if(old!=null)unload(remote,old);
            // Discard old level objects/identities, including identical-dimension world transfers.
            for(var loaded:remote.getAllLoadedLevels()){
                var wrapper=loaded.getLevelWrapper();
                if(wrapper instanceof TflLevel tfl && (tfl.getWrappedMcObject()!=mc.level || !tfl.namespace.equals(namespace)))unload(remote,tfl);
            }
            var paths=((DhSaveCacheAccess)remote.saveStructure).tfl$levelPaths();
            paths.keySet().removeIf(wrapper->wrapper instanceof TflLevel tfl && (tfl.getWrappedMcObject()!=mc.level || !tfl.namespace.equals(namespace)));
        }
        if(replayLevel.get()==mc.level && mc.player!=null){
            // Re-submit only chunks that the client already has, with a small per-tick budget.
            int side=radius*2+1,total=side*side;
            IClientLevelWrapper wrapper=ClientLevelWrapper.getWrapper(mc.level);
            for(int n=0;n<4 && cursor<total;n++,cursor++){
                int x=centerX+cursor%side-radius,z=centerZ+cursor/side-radius;
                var chunk=mc.level.getChunkSource().getChunk(x,z,ChunkStatus.FULL,false);
                if(chunk!=null)SharedApi.INSTANCE.applyChunkUpdate(new ChunkWrapper(chunk,wrapper),wrapper,true);
            }
            if(cursor>=total)replayLevel=new WeakReference<>(null);
        }
    }
    private static void unload(DhClientWorld world,TflLevel level){world.unloadLevel(level);((DhSaveCacheAccess)world.saveStructure).tfl$levelPaths().remove(level);}
    private static String hash(String value){try{return java.util.HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8)));}catch(java.security.NoSuchAlgorithmException e){throw new IllegalStateException(e);}}
}
*///?}
