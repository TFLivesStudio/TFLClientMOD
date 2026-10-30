package dev.theredstonee.trsclient.ornithe;

import static org.junit.jupiter.api.Assertions.*;

import dev.diorama.bake.ChunkSource;
import dev.diorama.data.ChunkSnapshot;
import java.util.*;
import org.junit.jupiter.api.Test;

class TrackedChunksTest {
  static ChunkSnapshot snapshot(int x) {
    return new ChunkSnapshot(x, 0, new int[16][], new byte[16][], new byte[16][], 256, 0, new ChunkSnapshot.Tints(new int[256],new int[256],new int[256],new int[256]));
  }

  @Test
  void unrelatedEditsStayCachedButChangedAndPreviouslyMissingDependenciesInvalidate() {
    Map<Long, ChunkSnapshot> map = new HashMap<>();
    map.put(ChunkSnapshot.key(0, 0), snapshot(0));
    ChunkSource source =
        new ChunkSource() {
          public ChunkSnapshot chunk(int x, int z) {
            return map.get(ChunkSnapshot.key(x, z));
          }

          public int minY() {
            return 0;
          }

          public int maxY() {
            return 256;
          }

          public int topY() {
            return 256;
          }

          public boolean isSeen(int x, int y, int z) {
            return chunk(x >> 4, z >> 4) != null;
          }
        };
    TrackedChunks read = new TrackedChunks(source);
    ChunkSnapshot first = read.chunk(0, 0);
    assertNull(read.chunk(1, 0));
    var dependencies = read.dependencies();
    map.put(ChunkSnapshot.key(9, 0), snapshot(9));
    assertTrue(dependencies.current(map));
    map.put(ChunkSnapshot.key(1, 0), snapshot(1));
    assertFalse(dependencies.current(map));
    assertNull(read.chunk(1, 0));
    map.remove(ChunkSnapshot.key(1, 0));
    assertTrue(dependencies.current(map));
    map.put(ChunkSnapshot.key(0, 0), snapshot(0));
    assertTrue(dependencies.current(map),"Identical received terrain reuses the bake");
    int[][] changedStates=new int[16][];changedStates[0]=new int[4096];changedStates[0][0]=1;
    map.put(ChunkSnapshot.key(0,0),new ChunkSnapshot(0,0,changedStates,new byte[16][],new byte[16][],256,0,new ChunkSnapshot.Tints(new int[256],new int[256],new int[256],new int[256])));
    assertFalse(dependencies.current(map));
    assertSame(first, read.chunk(0, 0));
  }

  @Test
  void persistedHashesKeepUnloadedTerrainUntilNewReceivedDataInvalidatesIt() throws Exception {
    ChunkSnapshot snapshot=snapshot(2);Map<Long,Long> hashes=new HashMap<>();hashes.put(2L,snapshot.fingerprint());hashes.put(3L,0L);
    var dependencies=new TrackedChunks.Dependencies(hashes);var bytes=new java.io.ByteArrayOutputStream();dependencies.write(new java.io.DataOutputStream(bytes));var loaded=TrackedChunks.Dependencies.read(new java.io.DataInputStream(new java.io.ByteArrayInputStream(bytes.toByteArray())));
    Map<Long,ChunkSnapshot> current=new HashMap<>();assertTrue(loaded.current(current));current.put(2L,snapshot(2));assertTrue(loaded.current(current));current.put(3L,snapshot(3));assertFalse(loaded.current(current));
  }
}
