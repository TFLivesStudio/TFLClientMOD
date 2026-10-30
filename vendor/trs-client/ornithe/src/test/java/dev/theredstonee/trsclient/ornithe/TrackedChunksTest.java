package dev.theredstonee.trsclient.ornithe;

import static org.junit.jupiter.api.Assertions.*;

import dev.diorama.bake.ChunkSource;
import dev.diorama.data.ChunkSnapshot;
import java.util.*;
import org.junit.jupiter.api.Test;

class TrackedChunksTest {
  static ChunkSnapshot snapshot(int x) {
    return new ChunkSnapshot(x, 0, new int[16][], new byte[16][], new byte[16][], 256, 0, null);
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
    assertFalse(dependencies.current(map));
    assertSame(first, read.chunk(0, 0));
  }

  @Test
  void aCollectedDependencyIsNeverMistakenForAnOriginallyMissingChunk() {
    ChunkSnapshot snapshot = snapshot(2);
    java.lang.ref.WeakReference<ChunkSnapshot> ref = new java.lang.ref.WeakReference<>(snapshot);
    Map<Long, java.lang.ref.WeakReference<ChunkSnapshot>> refs = new HashMap<>();
    refs.put(2L, ref);
    var dependencies = new TrackedChunks.Dependencies(refs);
    Map<Long, ChunkSnapshot> current = new HashMap<>();
    current.put(2L, snapshot);
    assertTrue(dependencies.current(current));
    ref.clear();
    assertFalse(dependencies.current(current));
    current.clear();
    assertFalse(dependencies.current(current));
  }
}
