package dev.diorama.bake;
import dev.diorama.data.ChunkSnapshot;
import dev.theredstonee.trsclient.core.map.QuarterTurn;
import java.util.*;
/** A job-local rotated view of immutable world snapshots; original chunk coordinates never change. */
public final class RotatedSource implements ChunkSource {
 private final ChunkSource source;private final int turns;private final Map<Long,ChunkSnapshot> cache=new HashMap<>();
 public RotatedSource(ChunkSource source,int turns){this.source=source;this.turns=turns&3;}
 public ChunkSnapshot chunk(int u,int d){long key=ChunkSnapshot.key(u,d);if(!cache.containsKey(key)){ChunkSnapshot s=source.chunk(QuarterTurn.blockX(u,d,turns),QuarterTurn.blockZ(u,d,turns));cache.put(key,s==null?null:s.rotated(turns));}return cache.get(key);}
 public int minY(){return source.minY();}public int maxY(){return source.maxY();}public int topY(){return source.topY();}public int roof(){return source.roof();}public boolean isSeen(int u,int y,int d){return source.isSeen(QuarterTurn.blockX(u,d,turns),y,QuarterTurn.blockZ(u,d,turns));}
}
