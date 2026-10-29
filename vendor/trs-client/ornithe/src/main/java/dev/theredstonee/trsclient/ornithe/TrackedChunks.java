package dev.theredstonee.trsclient.ornithe;
import dev.diorama.bake.ChunkSource;
import dev.diorama.data.ChunkSnapshot;
import java.util.*;
/** Per-bake memoization includes missing chunks and captures dimension metadata on the client. */
final class TrackedChunks implements ChunkSource {
 private final ChunkSource source;
 private final Map<Long,ChunkSnapshot> read=new HashMap<>();
 private final int min,max,top,roof;
 TrackedChunks(ChunkSource source){this.source=source;min=source.minY();max=source.maxY();top=source.topY();roof=source.roof();}
 public ChunkSnapshot chunk(int x,int z){long key=ChunkSnapshot.key(x,z);if(!read.containsKey(key))read.put(key,source.chunk(x,z));return read.get(key);}
 public int minY(){return min;}public int maxY(){return max;}public int topY(){return top;}public int roof(){return roof;}
 public boolean isSeen(int x,int y,int z){return chunk(x>>4,z>>4)!=null;}
 boolean current(Map<Long,ChunkSnapshot> snapshots){for(var entry:read.entrySet())if(snapshots.get(entry.getKey())!=entry.getValue())return false;return true;}
}
