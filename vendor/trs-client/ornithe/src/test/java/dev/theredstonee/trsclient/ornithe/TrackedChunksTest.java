package dev.theredstonee.trsclient.ornithe;
import dev.diorama.bake.ChunkSource;
import dev.diorama.data.ChunkSnapshot;
import java.util.*;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
class TrackedChunksTest {
 static ChunkSnapshot snapshot(int x){return new ChunkSnapshot(x,0,new int[16][],new byte[16][],new byte[16][],256,0,null);}
 @Test void unrelatedEditsStayCachedButChangedAndPreviouslyMissingDependenciesInvalidate(){
  Map<Long,ChunkSnapshot> map=new HashMap<>();map.put(ChunkSnapshot.key(0,0),snapshot(0));
  ChunkSource source=new ChunkSource(){public ChunkSnapshot chunk(int x,int z){return map.get(ChunkSnapshot.key(x,z));}public int minY(){return 0;}public int maxY(){return 256;}public int topY(){return 256;}public boolean isSeen(int x,int y,int z){return chunk(x>>4,z>>4)!=null;}};
  TrackedChunks read=new TrackedChunks(source);ChunkSnapshot first=read.chunk(0,0);assertNull(read.chunk(1,0));
  map.put(ChunkSnapshot.key(9,0),snapshot(9));assertTrue(read.current(map));
  map.put(ChunkSnapshot.key(1,0),snapshot(1));assertFalse(read.current(map));assertNull(read.chunk(1,0));
  map.remove(ChunkSnapshot.key(1,0));assertTrue(read.current(map));
  map.put(ChunkSnapshot.key(0,0),snapshot(0));assertFalse(read.current(map));assertSame(first,read.chunk(0,0));
 }
}
