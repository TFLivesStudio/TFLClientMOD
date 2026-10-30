package dev.theredstonee.trsclient.ornithe;

import dev.diorama.bake.ChunkSource;
import dev.diorama.data.ChunkSnapshot;
import java.util.*;

/** Per-bake memoization includes missing chunks and captures dimension metadata on the client. */
final class TrackedChunks implements ChunkSource {
  private final ChunkSource source;
  private final Map<Long, ChunkSnapshot> read = new HashMap<>();
  private final int min, max, top, roof;

  TrackedChunks(ChunkSource source) {
    this.source = source;
    min = source.minY();
    max = source.maxY();
    top = source.topY();
    roof = source.roof();
  }

  public ChunkSnapshot chunk(int x, int z) {
    long key = ChunkSnapshot.key(x, z);
    if (!read.containsKey(key)) read.put(key, source.chunk(x, z));
    return read.get(key);
  }

  public int minY() {
    return min;
  }

  public int maxY() {
    return max;
  }

  public int topY() {
    return top;
  }

  public int roof() {
    return roof;
  }

  public boolean isSeen(int x, int y, int z) {
    return chunk(x >> 4, z >> 4) != null;
  }

  Dependencies dependencies() {
    Map<Long, Long> values = new HashMap<>();
    for (var e : read.entrySet())
      values.put(e.getKey(), e.getValue() == null ? 0L : e.getValue().fingerprint());
    return new Dependencies(values);
  }

  static final class Dependencies {
    private final Map<Long, Long> values;

    Dependencies(Map<Long, Long> values) {
      this.values = values;
    }

    boolean current(Map<Long, ChunkSnapshot> snapshots) {
      for (var e : values.entrySet()) {
        ChunkSnapshot now = snapshots.get(e.getKey());
        // Persisted known terrain stays visible until received data proves that it changed.
        if (now != null && (e.getValue() == 0 || now.fingerprint() != e.getValue())) return false;
      }
      return true;
    }

    void write(java.io.DataOutput out) throws java.io.IOException {
      out.writeInt(values.size());
      for (var e : values.entrySet()) {
        out.writeLong(e.getKey());
        out.writeLong(e.getValue());
      }
    }

    static Dependencies read(java.io.DataInput in) throws java.io.IOException {
      int n = in.readInt();
      if (n < 0 || n > 4096) throw new java.io.IOException("Invalid tile dependencies");
      Map<Long, Long> values = new HashMap<>();
      for (int i = 0; i < n; i++) values.put(in.readLong(), in.readLong());
      return new Dependencies(values);
    }
  }
}
