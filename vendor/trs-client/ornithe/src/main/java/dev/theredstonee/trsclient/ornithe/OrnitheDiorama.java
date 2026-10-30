package dev.theredstonee.trsclient.ornithe;

import dev.diorama.Projection;
import dev.diorama.bake.*;
import dev.diorama.data.ChunkSnapshot;
import dev.theredstonee.trsclient.core.ui.*;
import java.io.*;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.*;
import net.minecraft.block.Block;
import net.minecraft.block.material.Material;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.chunk.WorldChunk;

/** Diorama's original voxel ray tracer over immutable received 1.8.9 chunk sections. */
public final class OrnitheDiorama implements ChunkSource, AutoCloseable {
  private final OrnitheClient client;
  private final MapGlState drawState = new MapGlState();
  final DioramaMarkers markers;
  private final dev.theredstonee.trsclient.core.map.MobHeads mobHeads =
      new dev.theredstonee.trsclient.core.map.MobHeads();
  private ConcurrentHashMap<Long, ChunkSnapshot> chunks = new ConcurrentHashMap<>();
  private LinkedHashMap<TileKey, Tile> tiles = new LinkedHashMap<>(32, .75f, true);
  private final Set<Job> baking = ConcurrentHashMap.newKeySet();

  private record Job(long epoch, TileKey key) {}

  private final ArrayBlockingQueue<Result> completed = new ArrayBlockingQueue<>(8);
  private final ThreadPoolExecutor worker =
      new ThreadPoolExecutor(
          2,
          2,
          10,
          TimeUnit.SECONDS,
          new ArrayBlockingQueue<>(16),
          r -> {
            Thread t = new Thread(r, "TFL-Diorama-bake");
            t.setDaemon(true);
            return t;
          },
          new ThreadPoolExecutor.AbortPolicy());
  private final ThreadPoolExecutor disk =
      new ThreadPoolExecutor(
          1,
          1,
          10,
          TimeUnit.SECONDS,
          new ArrayBlockingQueue<>(256),
          r -> {
            Thread t = new Thread(r, "TFL-Diorama-store");
            t.setDaemon(true);
            return t;
          },
          new ThreadPoolExecutor.DiscardPolicy());
  private HashMap<Long, Long> captured = new HashMap<>();
  private Set<Long> reading = ConcurrentHashMap.newKeySet(), stored = ConcurrentHashMap.newKeySet();
  private final ArrayBlockingQueue<Loaded> restored = new ArrayBlockingQueue<>(64);

  private record Loaded(long epoch, ChunkSnapshot snapshot) {}

  private record Cached(
      ConcurrentHashMap<Long, ChunkSnapshot> chunks,
      LinkedHashMap<TileKey, Tile> tiles,
      Set<Long> stored) {}

  private final LinkedHashMap<String, Cached> worlds = new LinkedHashMap<>(4, .75f, true);

  private void switchCache(String next) {
    if (!identity.isEmpty()) worlds.put(identity, new Cached(chunks, tiles, stored));
    Cached old = worlds.remove(next);
    if (old != null) worldCacheHits++;
    chunks = old == null ? new ConcurrentHashMap<>() : old.chunks;
    tiles = old == null ? new LinkedHashMap<>(32, .75f, true) : old.tiles;
    stored = old == null ? ConcurrentHashMap.newKeySet() : old.stored;
    captured = new HashMap<>();
    reading = ConcurrentHashMap.newKeySet();
    epoch++;
    revision++;
    baking.clear();
    worker.getQueue().clear();
    completed.clear();
    restored.clear();
    if (fog != null) fog.close();
    fog = null;
    maskAt = 0;
    actorCount = 0;
    while (worlds.size() > 2) {
      var iterator = worlds.entrySet().iterator();
      var entry = iterator.next();
      iterator.remove();
      for (Tile t : entry.getValue().tiles.values())
        if (t.texture != null) client.canvas.release(t.texture);
    }
  }

  private final dev.theredstonee.trsclient.core.map.MapEntity[] actors =
      new dev.theredstonee.trsclient.core.map.MapEntity[128];
  private int actorCount;
  private dev.theredstonee.trsclient.core.map.volume.CloudQuality quality;
  private long looksHash;
  public volatile long diskTileHits;
  public long worldCacheHits;
  private final BlockLooks[] rotatedLooks = new BlockLooks[4];

  public int rotation() {
    return client.mapPlatform.catalog().turns();
  }

  private BlockLooks looks;
  private Object world;
  private String identity = "";
  private volatile Path folder;
  private final int[] knownPrefix = new int[257 * 257], unknownPrefix = new int[257 * 257];
  private final int[] coverageBuffer = new int[256 * 256];
  private DioramaFog fog;
  private long maskAt;
  private double oldMaskU, oldMaskV, oldExtent;
  private long maskRevision, maskContentRevision;
  private int oldMaskLayer = Integer.MIN_VALUE, oldMaskLod = -1;
  private volatile long revision, epoch;
  private long lastCapture;
  private boolean closed;
  public volatile long bakedTiles, bakeNanos;
  public long captureNanos,
      captureCount,
      maskNanos,
      maskBuilds,
      uploadNanos,
      uploadCount,
      drawNanos,
      drawCount;
  public volatile String failure = "";

  private record Result(
      TileKey key, int[] pixels, TrackedChunks.Dependencies dependencies, long epoch) {}

  private static final class Tile {
    TextureRef texture;
    long checkedRevision = -1, lastRequest;
    boolean dirty;
    TrackedChunks.Dependencies dependencies;
    byte[] alpha;
  }

  private boolean stale(Tile tile) {
    if (tile.checkedRevision != revision) {
      tile.checkedRevision = revision;
      tile.dirty = tile.dependencies == null || !tile.dependencies.current(chunks);
    }
    return tile.dirty;
  }

  public OrnitheDiorama(OrnitheClient client) {
    this.client = client;
    markers = new DioramaMarkers(client);
    client.map.palette().setResources((ns, path) -> client.mapPlatform.readResource(ns, path));
    for (int i = 0; i < actors.length; i++)
      actors[i] = new dev.theredstonee.trsclient.core.map.MapEntity();
  }

  public ChunkSnapshot chunk(int x, int z) {
    return lookup(chunks, folder, stored, reading, epoch, x, z);
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

  public int roof() {
    return client.mapPlatform.dimension().equals("dim-1") ? 128 : NO_ROOF;
  }

  public boolean isSeen(int x, int y, int z) {
    return chunks.containsKey(ChunkSnapshot.key(x >> 4, z >> 4));
  }

  public int snapshotCount() {
    return chunks.size();
  }

  public int autoLayer() {
    if (client.modules.minimapCaveMode.get()
            == dev.theredstonee.trsclient.core.module.TrsModules.CaveMode.OFF
        || !client.map.fairPlay().caveAllowed(roof() != NO_ROOF)) return TileKey.SURFACE;
    return client.mapPlatform.skyLight() < 4 && roof() == NO_ROOF
        ? Math.min(255, (int) client.mapPlatform.y() + 16)
        : TileKey.SURFACE;
  }

  public void tick() {
    if (closed) return;
    String next = client.mapPlatform.worldKey() + "/" + client.mapPlatform.dimension();
    if (world != client.mc.world || !next.equals(identity)) {
      switchCache(next);
      world = client.mc.world;
      identity = next;
      folder =
          client
              .mc
              .gameDir
              .toPath()
              .resolve("tfl-maps")
              .resolve(
                  UUID.nameUUIDFromBytes(next.getBytes(java.nio.charset.StandardCharsets.UTF_8))
                      .toString());
      if (world != null) {
        Path home = folder;
        long session = epoch;
        var sessionStored = stored;
        disk.execute(
            () -> {
              if (Files.isDirectory(home))
                try (var files = Files.newDirectoryStream(home, "*.bin.gz")) {
                  for (Path file : files) {
                    if (session != epoch) break;
                    String[] parts =
                        file.getFileName().toString().replace(".bin.gz", "").split("_");
                    if (parts.length == 2)
                      try {
                        sessionStored.add(
                            ChunkSnapshot.key(
                                Integer.parseInt(parts[0]), Integer.parseInt(parts[1])));
                      } catch (NumberFormatException ignored) {
                      }
                  }
                } catch (IOException ignored) {
                }
            });
        if (looks == null) looks = buildLooks();
        client.map.fairPlay().resetServer();
        client.map.fairPlay().onServerText(client.mapPlatform.serverMotd());
      }
    }
    if (world == null || client.mc.player == null) return;
    var requestedQuality = client.modules.worldMapCloudQuality.get();
    if (quality != requestedQuality) {
      quality = requestedQuality;
      if (fog != null) fog.close();
      fog = null;
      maskAt = 0;
    }
    client.map.fairPlay().setManual(client.modules.minimapFairPlay.get());
    actorCount = 0;
    client.mapPlatform.entities(
        () -> actorCount >= actors.length ? null : actors[actorCount++],
        128,
        !client.map.fairPlay().radarThroughWalls());
    for (int i = 0; i < 4; i++) {
      Loaded loaded = restored.poll();
      if (loaded == null) break;
      if (loaded.epoch != epoch) continue;
      ChunkSnapshot s = loaded.snapshot;
      long k = ChunkSnapshot.key(s.chunkX, s.chunkZ);
      if (chunks.putIfAbsent(k, s) == null) revision++;
    }
    if (disk.getQueue().remainingCapacity() < 8) return;
    if (System.nanoTime() - lastCapture < 20_000_000L) return;
    lastCapture = System.nanoTime();
    int px = (int) Math.floor(client.mc.player.x) >> 4,
        pz = (int) Math.floor(client.mc.player.z) >> 4;
    int radius = Math.min(12, client.mc.options.viewDistance);
    long now = System.currentTimeMillis();
    for (int ring = 0; ring <= radius; ring++)
      for (int dx = -ring; dx <= ring; dx++)
        for (int dz = -ring; dz <= ring; dz++) {
          if (Math.abs(dx) != ring && Math.abs(dz) != ring) continue;
          int x = px + dx, z = pz + dz;
          long key = ChunkSnapshot.key(x, z);
          if (now - captured.getOrDefault(key, 0L) < 15000) continue;
          var received = client.mc.world.getChunkSource().getChunk(x, z);
          if (received == null || received instanceof net.minecraft.world.chunk.EmptyChunk)
            continue;
          long captureStart = System.nanoTime();
          ChunkSnapshot snapshot = capture(received, x, z);
          captureNanos += System.nanoTime() - captureStart;
          captureCount++;
          if (snapshot == null || world != client.mc.world) continue;
          captured.put(key, now);
          ChunkSnapshot previous = chunks.get(key);
          if (snapshot.sameContent(previous)) return;
          chunks.put(key, snapshot);
          captured.put(key, now);
          revision++;
          if (chunks.size() > 384) {
            Long oldest =
                chunks.keySet().stream()
                    .filter(
                        k ->
                            Math.abs((int) (k >> 32) - px) > radius + 2
                                || Math.abs((int) (long) k - pz) > radius + 2)
                    .findFirst()
                    .orElse(null);
            if (oldest != null) {
              chunks.remove(oldest);
              reading.remove(oldest);
            }
          }
          Path destination = folder.resolve(x + "_" + z + ".bin.gz");
          disk.execute(() -> save(destination, snapshot));
          return;
        }
  }

  private BlockLooks buildLooks() {
    BlockLooks l = new BlockLooks();
    LegacyBlockGeometry geometry = new LegacyBlockGeometry();
    var shapes = new ArrayList<float[]>();
    shapes.add(new float[] {0, 0, 0, 1, 1, 1});
    var shapeIds = new HashMap<String, Integer>();
    for (Block block : Block.REGISTRY)
      for (int meta = 0; meta < 16; meta++)
        try {
          var state = block.getStateFromMetadata(meta);
          int id = Block.STATE_REGISTRY.getId(state);
          if (id < 0 || id >= 65536) continue;
          Material material = block.getMaterial();
          l.kind[id] =
              material == Material.AIR
                  ? BlockLooks.AIR
                  : material == Material.WATER
                      ? BlockLooks.WATER
                      : material == Material.LAVA
                          ? BlockLooks.LAVA
                          : material == Material.GLASS
                              ? BlockLooks.GLASS
                              : material.isSolid() ? BlockLooks.SOLID : BlockLooks.PLANT;
          if (l.kind[id] == BlockLooks.SOLID || l.kind[id] == BlockLooks.GLASS)
            try {
              float[] boxes = geometry.boxes(state);
              if (!Arrays.equals(boxes, shapes.get(0))) {
                String key = Arrays.toString(boxes);
                Integer shape = shapeIds.get(key);
                if (shape == null) {
                  shape = shapes.size();
                  shapeIds.put(key, shape);
                  shapes.add(boxes);
                }
                l.shape[id] = shape;
              }
            } catch (RuntimeException ignored) {
            }
          int rgb = block.getMapColor(state).color;
          try {
            var sprite =
                client.mc.getBlockRenderDispatcher().getModelShaper().getParticleIcon(state);
            if (sprite.getFrameCount() > 0)
              rgb =
                  dev.theredstonee.trsclient.core.map.TexturePalette.average(
                      sprite.getFrame(0)[0], sprite.getWidth(), sprite.getHeight());
          } catch (RuntimeException ignored) {
          }
          l.top[id] = l.side[id] = rgb;
          l.emission[id] = (byte) block.getLight();
          l.spawnOn[id] = block.isOpaque();
          l.spawnIn[id] = material == Material.AIR;
        } catch (RuntimeException ignored) {
        }
    l.shapes = shapes.toArray(new float[0][]);
    looksHash =
        (Arrays.hashCode(l.top) * 31L + Arrays.deepHashCode(l.shapes)) * 31
            + Arrays.hashCode(l.kind);
    looksHash = looksHash * 31 + Arrays.hashCode(l.emission);
    looksHash = looksHash * 31 + Double.doubleToLongBits(Projection.sin());
    Arrays.fill(rotatedLooks, null);
    rotatedLooks[0] = l;
    return l;
  }

  private ChunkSnapshot capture(WorldChunk chunk, int x, int z) {
    int[][] states = new int[16][];
    byte[][] block = new byte[16][], sky = new byte[16][];
    if (chunk == null) return null;
    var sections = chunk.getSections();
    if (sections == null) return null;
    boolean nether = client.mapPlatform.dimension().equals("dim-1");
    for (int i = 0; i < Math.min(16, sections.length); i++) {
      var section = sections[i];
      if (section == null) continue;
      char[] source = section.getBlockStates();
      if (source == null || source.length != 4096) return null;
      var blockStorage = section.getBlockLightStorage();
      var skyStorage = section.getSkyLightStorage();
      states[i] = new int[4096];
      block[i] = new byte[4096];
      sky[i] = new byte[4096];
      for (int j = 0; j < 4096; j++) {
        states[i][j] = source[j];
        block[i][j] =
            (byte) (blockStorage == null ? 0 : blockStorage.get(j & 15, j >> 8, j >> 4 & 15));
        sky[i][j] =
            (byte)
                (nether
                    ? 0
                    : skyStorage == null ? 15 : skyStorage.get(j & 15, j >> 8, j >> 4 & 15));
      }
    }
    int[] grass = new int[256], foliage = new int[256], water = new int[256];
    Arrays.fill(water, 0x3F76E4);
    int top = 0, floor = 256;
    for (int dz = 0; dz < 16; dz++)
      for (int dx = 0; dx < 16; dx++) {
        int y = chunk.getHeight(dx, dz);
        top = Math.max(top, y);
        floor = Math.min(floor, y);
        var pos = new BlockPos((x << 4) + dx, y, (z << 4) + dz);
        var biome = client.mc.world.getBiome(pos);
        int color = biome == null ? 0x91BD59 : biome.getGrassColor(pos);
        grass[dz * 16 + dx] = color;
        foliage[dz * 16 + dx] = color;
      }
    return new ChunkSnapshot(
        x,
        z,
        states,
        block,
        sky,
        top,
        floor,
        new ChunkSnapshot.Tints(grass, foliage, foliage, water));
  }

  public net.minecraft.util.math.BlockPos pick(double u, double v, int layer) {
    if (looks == null) return null;
    int q = rotation();
    if (rotatedLooks[q] == null) rotatedLooks[q] = looks.rotated(q);
    var spot =
        MapPick.standingSpot(new RotatedSource(this, q), rotatedLooks[q], u, v, layer, false);
    return spot == null
        ? null
        : new BlockPos(
            dev.theredstonee.trsclient.core.map.QuarterTurn.blockX(spot.getX(), spot.getZ(), q),
            spot.getY(),
            dev.theredstonee.trsclient.core.map.QuarterTurn.blockZ(spot.getX(), spot.getZ(), q));
  }

  public void draw(
      Canvas c,
      int x,
      int y,
      int width,
      int height,
      double centerX,
      double centerY,
      double centerZ,
      double pixelsPerBlock) {
    draw(c, x, y, width, height, centerX, centerY, centerZ, pixelsPerBlock, TileKey.SURFACE);
  }

  public void changed(int x, int z) {
    captured.remove(ChunkSnapshot.key(x, z));
  }

  public void reload() {
    markers.reload();
    for (Cached cache : worlds.values())
      for (Tile t : cache.tiles.values()) if (t.texture != null) client.canvas.release(t.texture);
    worlds.clear();
    epoch++;
    reading.clear();
    restored.clear();
    if (world != null) looks = buildLooks();
    for (Tile tile : tiles.values()) if (tile.texture != null) client.canvas.release(tile.texture);
    tiles.clear();
    completed.clear();
    if (fog != null) fog.close();
    fog = null;
    maskAt = 0;
  }

  public boolean fogHealthy() {
    return fog != null && fog.healthy();
  }

  public void draw(
      Canvas c,
      int x,
      int y,
      int width,
      int height,
      double centerX,
      double centerY,
      double centerZ,
      double pixelsPerBlock,
      int layer) {
    long drawStart = System.nanoTime();
    int turns = rotation();
    double cx = dev.theredstonee.trsclient.core.map.QuarterTurn.u(centerX, centerZ, turns),
        cz = dev.theredstonee.trsclient.core.map.QuarterTurn.depth(centerX, centerZ, turns);
    centerX = cx;
    centerZ = cz;
    try (var ignored = drawState.begin()) {
      upload();
      c.scissor(x, y, x + width, y + height);
      c.fill(x, y, x + width, y + height, 0xFF8796A8);
      if (looks != null) {
        double v = Projection.v(centerY, centerZ);
        int lod =
            Math.max(
                0,
                Math.min(
                    TileKey.MAX_LOD,
                    (int) Math.floor(Math.log(.5 / pixelsPerBlock) / Math.log(2))));
        double units = Projection.TILE_UNITS * (1 << lod);
        double left = centerX - width / (2 * pixelsPerBlock),
            top = v - height / (2 * pixelsPerBlock);
        int minU = (int) Math.floor(left / units),
            maxU = (int) Math.floor((left + width / pixelsPerBlock) / units);
        int minV = (int) Math.floor(top / units),
            maxV = (int) Math.floor((top + height / pixelsPerBlock) / units);
        int requested = 0;
        for (int tv = minV; tv <= maxV; tv++)
          for (int tu = minU; tu <= maxU; tu++) {
            TileKey key = new TileKey(layer, tu, tv, lod, turns);
            Tile tile = tiles.get(key);
            if (tile != null && tile.texture != null) {
              float px = (float) (x + (key.minU() - left) * pixelsPerBlock),
                  py = (float) (y + (key.minV() - top) * pixelsPerBlock);
              Affine.image(
                  c,
                  tile.texture,
                  px,
                  py,
                  (float) (units * pixelsPerBlock + .2),
                  (float) (units * pixelsPerBlock + .2),
                  0,
                  0,
                  Projection.TILE_PIXELS,
                  Projection.TILE_PIXELS,
                  0xFFFFFFFF);
            }
            if (requested < 16
                && (tile == null
                    || stale(tile) && System.nanoTime() - tile.lastRequest > 1_000_000_000L)
                && baking.size() < 8) {
              if (tile != null) tile.lastRequest = System.nanoTime();
              request(key);
              requested++;
            }
          }
      }
      // The same eight-layer height-field shader as modern Diorama, at the UI frame rate.
      if (fog == null) fog = new DioramaFog(client);
      int maskLod =
          Math.max(
              0,
              Math.min(
                  TileKey.MAX_LOD, (int) Math.floor(Math.log(.5 / pixelsPerBlock) / Math.log(2))));
      double tileUnits = 128 * (1 << maskLod);
      double scaleLog = Math.min(7.99, Math.log(Math.max(1, .5 / pixelsPerBlock)) / Math.log(2));
      int steps = (int) Math.floor(scaleLog), fraction = (int) Math.floor((scaleLog - steps) * 16);
      double snap = 64.0 * (1 << (steps + 1));
      double visible = Math.max(width, height) / pixelsPerBlock;
      int code = 0;
      while (512 * (1 << code) < visible + 48 + snap && code < 12) code++;
      double extent = 512 * (1 << code), texel = extent / 256;
      double maskU = Math.floor((centerX - width / (2 * pixelsPerBlock) - 24) / snap) * snap,
          maskV =
              Math.floor(
                      (Projection.v(centerY, centerZ) - height / (2 * pixelsPerBlock) - 24) / snap)
                  * snap;
      int[] mask = null;
      long now = System.nanoTime();
      if (maskAt == 0
          || maskU != oldMaskU
          || maskV != oldMaskV
          || extent != oldExtent
          || maskRevision != maskContentRevision
          || layer != oldMaskLayer
          || maskLod != oldMaskLod) {
        mask = coverageBuffer;
        for (int my = 0; my < 256; my++)
          for (int mx = 0; mx < 256; mx++) {
            double u = maskU + (mx + .5) * texel, v = maskV + (my + .5) * texel;
            Tile tile =
                tiles.get(
                    new TileKey(
                        layer,
                        (int) Math.floor(u / tileUnits),
                        (int) Math.floor(v / tileUnits),
                        maskLod,
                        turns));
            int known = 0;
            if (tile != null && tile.alpha != null) {
              int tx = Math.floorMod((int) Math.floor(u * 4 / (1 << maskLod)), 512),
                  ty = Math.floorMod((int) Math.floor(v * 4 / (1 << maskLod)), 512);
              known = tile.alpha[ty * 512 + tx] & 255;
            }
            mask[my * 256 + mx] = 0xff000000 | known << 16;
          }
        flagMask(mask, texel, steps);
        maskAt = now;
        oldMaskU = maskU;
        oldMaskV = maskV;
        oldExtent = extent;
        maskRevision = maskContentRevision;
        oldMaskLayer = layer;
        oldMaskLod = maskLod;
        maskNanos += System.nanoTime() - now;
        maskBuilds++;
      }
      double left = centerX - width / (2 * pixelsPerBlock),
          top = Projection.v(centerY, centerZ) - height / (2 * pixelsPerBlock);
      if (quality != dev.theredstonee.trsclient.core.map.volume.CloudQuality.OFF)
        fog.draw(
            x,
            y,
            width,
            height,
            mask,
            (left - maskU) / extent,
            (top - maskV) / extent,
            width / pixelsPerBlock / extent,
            height / pixelsPerBlock / extent,
            code,
            steps,
            fraction,
            snap,
            maskU,
            maskV,
            layer != TileKey.SURFACE);
      boolean full = width > client.modules.minimapSize.getInt() + 16;
      if (full ? client.modules.worldMapWaypoints.get() : client.modules.minimapWaypoints.get())
        for (var point :
            client.waypoints.visible(
                client.mapPlatform.worldKey(), client.mapPlatform.dimension())) {
          if (client.mapPlatform.hiddenFolder(point.folder)) continue;
          int
              px =
                  (int)
                      Math.round(
                          x
                              + width / 2
                              + (dev.theredstonee.trsclient.core.map.QuarterTurn.u(
                                          point.x, point.z, turns)
                                      - centerX)
                                  * pixelsPerBlock),
              py =
                  (int)
                      Math.round(
                          y
                              + height / 2
                              + (Projection.v(
                                          point.y,
                                          dev.theredstonee.trsclient.core.map.QuarterTurn.depth(
                                              point.x, point.z, turns))
                                      - Projection.v(centerY, centerZ))
                                  * pixelsPerBlock);
          if (px >= x && py >= y && px < x + width && py < y + height)
            markers.waypoint(c, point, px, py, full);
        }
      var modules = client.modules;
      for (int i = 0; i < actorCount; i++) {
        var entity = actors[i];
        boolean player = entity.type == dev.theredstonee.trsclient.core.map.MapEntity.PLAYER;
        if (player && !(full ? modules.worldMapPlayers.get() : modules.minimapPlayers.get()))
          continue;
        if (!player
            && entity.type == dev.theredstonee.trsclient.core.map.MapEntity.HOSTILE
            && !(full ? modules.worldMapHostile.get() : modules.minimapHostile.get())) continue;
        if (!player
            && entity.type != dev.theredstonee.trsclient.core.map.MapEntity.HOSTILE
            && !(full ? modules.worldMapPassive.get() : modules.minimapPassive.get())) continue;
        int
            px =
                (int)
                    Math.round(
                        x
                            + width / 2
                            + (dev.theredstonee.trsclient.core.map.QuarterTurn.u(
                                        entity.x, entity.z, turns)
                                    - centerX)
                                * pixelsPerBlock),
            py =
                (int)
                    Math.round(
                        y
                            + height / 2
                            + (Projection.v(
                                        entity.y,
                                        dev.theredstonee.trsclient.core.map.QuarterTurn.depth(
                                            entity.x, entity.z, turns))
                                    - Projection.v(centerY, centerZ))
                                * pixelsPerBlock);
        if (px < x || py < y || px >= x + width || py >= y + height) continue;
        int color =
            player
                ? 0xff7dd3fc
                : entity.type == dev.theredstonee.trsclient.core.map.MapEntity.HOSTILE
                    ? 0xfffb7185
                    : 0xff86efac;
        SelectionStyle.rounded(c, px - 7, py - 7, 14, 14, 4, color);
        if (!player
            && modules.minimapMobIcons.get()
                == dev.theredstonee.trsclient.core.module.TrsModules.MobIcons.HEADS) {
          var head =
              mobHeads.head(entity.kind, client.map.palette(), client.canvas.resourceGeneration());
          if (head != null)
            Affine.image(
                c,
                head.texture,
                px - 6,
                py - 6,
                12,
                12,
                head.u,
                head.v,
                head.w,
                head.h,
                0xffffffff);
        }
        if (player && entity.skin != null) {
          Affine.image(c, entity.skin, px - 6, py - 6, 12, 12, 8, 8, 8, 8, 0xffffffff);
          Affine.image(c, entity.skin, px - 6, py - 6, 12, 12, 40, 8, 8, 8, 0xffffffff);
        } else if (player && client.mc.getNetworkHandler() != null) {
          var info = client.mc.getNetworkHandler().getOnlinePlayer(entity.uuid);
          if (info != null) {
            TextureRef skin = client.canvas.game(info.getSkinTexture().toString(), 64, 64);
            Affine.image(c, skin, px - 6, py - 6, 12, 12, 8, 8, 8, 8, 0xffffffff);
          }
        }
      }
      int
          playerX =
              (int)
                  Math.round(
                      x
                          + width / 2
                          + (dev.theredstonee.trsclient.core.map.QuarterTurn.u(
                                      client.mapPlatform.x(), client.mapPlatform.z(), turns)
                                  - centerX)
                              * pixelsPerBlock),
          playerY =
              (int)
                  Math.round(
                      y
                          + height / 2
                          + (Projection.v(
                                      client.mapPlatform.y(),
                                      dev.theredstonee.trsclient.core.map.QuarterTurn.depth(
                                          client.mapPlatform.x(), client.mapPlatform.z(), turns))
                                  - Projection.v(centerY, centerZ))
                              * pixelsPerBlock);
      markers.player(c, playerX, playerY, client.mapPlatform.yaw(), turns);
      c.noScissor();
    }
    drawNanos += System.nanoTime() - drawStart;
    drawCount++;
  }

  public Map<String, Object> metrics() {
    Map<String, Object> m = new LinkedHashMap<>();
    m.put("worldCacheHits", worldCacheHits);
    m.put("diskTileHits", diskTileHits);
    m.put("captureCount", captureCount);
    m.put("captureMeanMs", captureNanos / 1e6 / Math.max(1, captureCount));
    m.put("bakedTiles", bakedTiles);
    m.put("bakeMeanMs", bakeNanos / 1e6 / Math.max(1, bakedTiles));
    m.put("maskBuilds", maskBuilds);
    m.put("maskMeanMs", maskNanos / 1e6 / Math.max(1, maskBuilds));
    m.put("uploadCount", uploadCount);
    m.put("uploadMeanMs", uploadNanos / 1e6 / Math.max(1, uploadCount));
    m.put("drawCount", drawCount);
    m.put("drawMeanMs", drawNanos / 1e6 / Math.max(1, drawCount));
    m.put("queuedBakes", worker.getQueue().size());
    m.put("queuedUploads", completed.size());
    m.put("gpuTimingMeasured", false);
    return m;
  }

  private void flagMask(int[] mask, double texel, int scaleSteps) {
    java.util.Arrays.fill(knownPrefix, 0);
    java.util.Arrays.fill(unknownPrefix, 0);
    for (int y = 0; y < 256; y++) {
      int known = 0, unknown = 0;
      for (int x = 0; x < 256; x++) {
        int red = mask[y * 256 + x] >>> 16 & 255;
        known += red == 255 ? 1 : 0;
        unknown += red == 0 ? 1 : 0;
        knownPrefix[(y + 1) * 257 + x + 1] = knownPrefix[y * 257 + x + 1] + known;
        unknownPrefix[(y + 1) * 257 + x + 1] = unknownPrefix[y * 257 + x + 1] + unknown;
      }
    }
    int reach =
        Math.min(
            256,
            Math.max(2, (int) Math.ceil(Math.max(34, 9.0 * (1 << (scaleSteps + 1))) / texel + 8)));
    for (int y = 0; y < 256; y++)
      for (int x = 0; x < 256; x++) {
        int x0 = Math.max(0, x - reach),
            y0 = Math.max(0, y - reach),
            x1 = Math.min(256, x + reach + 1),
            y1 = Math.min(256, y + reach + 1),
            count = (x1 - x0) * (y1 - y0);
        int a = y1 * 257 + x1, b = y0 * 257 + x1, c = y1 * 257 + x0, d = y0 * 257 + x0;
        int known = knownPrefix[a] - knownPrefix[b] - knownPrefix[c] + knownPrefix[d],
            unknown = unknownPrefix[a] - unknownPrefix[b] - unknownPrefix[c] + unknownPrefix[d];
        if (known == count) mask[y * 256 + x] |= 0x0000ff00;
        else if (unknown == count) mask[y * 256 + x] |= 0x000000ff;
      }
  }

  private void request(TileKey key) {
    long session = epoch;
    Job job = new Job(session, key);
    if (!baking.add(job)) return;
    if (rotatedLooks[key.turns()] == null) rotatedLooks[key.turns()] = looks.rotated(key.turns());
    BlockLooks colors = rotatedLooks[key.turns()];
    var snapshots = chunks;
    int roof = roof();
    Path home = folder;
    var known = stored;
    var pending = reading;
    TrackedChunks source =
        new TrackedChunks(
            new ChunkSource() {
              public ChunkSnapshot chunk(int x, int z) {
                return lookup(snapshots, home, known, pending, session, x, z);
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

              public int roof() {
                return roof;
              }

              public boolean isSeen(int x, int y, int z) {
                return snapshots.containsKey(ChunkSnapshot.key(x >> 4, z >> 4));
              }
            });
    Path cache =
        home == null
            ? null
            : home.resolve("tiles-v1-" + Long.toHexString(looksHash))
                .resolve(
                    key.turns()
                        + "_"
                        + key.layer()
                        + "_"
                        + key.lod()
                        + "_"
                        + key.tileU()
                        + "_"
                        + key.tileV()
                        + ".gz");
    try {
      worker.execute(
          () -> {
            try {
              Result hit = readTile(cache, key, session);
              if (hit != null && hit.dependencies.current(snapshots)) {
                if (session == epoch) {
                  completed.offer(hit);
                  diskTileHits++;
                }
                return;
              }
              long start = System.nanoTime();
              TileImage image =
                  TileBaker.bake(key, new RotatedSource(source, key.turns()), colors, null, false);
              int[] pixels = image.mips()[0];
              for (int i = 0; i < pixels.length; i++) {
                int v = pixels[i];
                pixels[i] = v & 0xFF00FF00 | (v & 255) << 16 | (v >>> 16 & 255);
              }
              Result result = new Result(key, pixels, source.dependencies(), session);
              if (session == epoch) {
                completed.offer(result);
                synchronized (OrnitheDiorama.this) {
                  bakeNanos += System.nanoTime() - start;
                  bakedTiles++;
                }
                if (cache != null) disk.execute(() -> saveTile(cache, result));
              }
            } catch (Throwable e) {
              failure = e.toString();
            } finally {
              baking.remove(job);
            }
          });
    } catch (RejectedExecutionException e) {
      baking.remove(job);
    }
  }

  private ChunkSnapshot lookup(
      ConcurrentHashMap<Long, ChunkSnapshot> snapshots,
      Path home,
      Set<Long> known,
      Set<Long> pending,
      long session,
      int x,
      int z) {
    long key = ChunkSnapshot.key(x, z);
    ChunkSnapshot present = snapshots.get(key);
    if (present == null
        && session == epoch
        && home != null
        && known.contains(key)
        && pending.size() < 4096
        && disk.getQueue().remainingCapacity() > 16
        && pending.add(key))
      disk.execute(
          () -> {
            try {
              ChunkSnapshot saved = load(home.resolve(x + "_" + z + ".bin.gz"));
              if (saved != null && session == epoch) restored.offer(new Loaded(session, saved));
            } finally {
              pending.remove(key);
            }
          });
    return present;
  }

  private Result readTile(Path path, TileKey key, long session) {
    if (path == null || !Files.isRegularFile(path)) return null;
    try (var in =
        new DataInputStream(new java.util.zip.GZIPInputStream(Files.newInputStream(path)))) {
      if (in.readInt() != 0x54465431) return null;
      var dependencies = TrackedChunks.Dependencies.read(in);
      int[] pixels = new int[Projection.TILE_PIXELS * Projection.TILE_PIXELS];
      for (int i = 0; i < pixels.length; i++) pixels[i] = in.readInt();
      return new Result(key, pixels, dependencies, session);
    } catch (IOException | RuntimeException invalid) {
      return null;
    }
  }

  private void saveTile(Path path, Result result) {
    try {
      Files.createDirectories(path.getParent());
      Path tmp = path.resolveSibling(path.getFileName() + ".tmp");
      try (var out =
          new DataOutputStream(new java.util.zip.GZIPOutputStream(Files.newOutputStream(tmp)))) {
        out.writeInt(0x54465431);
        result.dependencies.write(out);
        for (int pixel : result.pixels) out.writeInt(pixel);
      }
      Files.move(tmp, path, StandardCopyOption.REPLACE_EXISTING);
    } catch (IOException e) {
      failure = "Tile cache: " + e.getMessage();
    }
  }

  private void upload() {
    Result r;
    int uploaded = 0;
    while (uploaded++ < 1 && (r = completed.poll()) != null) {
      if (r.epoch != epoch) continue;
      long uploadStart = System.nanoTime();
      maskContentRevision++;
      Tile tile = tiles.computeIfAbsent(r.key, k -> new Tile());
      tile.texture =
          client.canvas.upload(
              "diorama/world-"
                  + folder.getFileName()
                  + "/tile-q"
                  + r.key.turns()
                  + "-"
                  + r.key.layer()
                  + "-"
                  + r.key.lod()
                  + "-"
                  + r.key.tileU()
                  + "-"
                  + r.key.tileV(),
              Projection.TILE_PIXELS,
              Projection.TILE_PIXELS,
              r.pixels);
      tile.dependencies = r.dependencies;
      tile.checkedRevision = -1;
      tile.alpha = new byte[r.pixels.length];
      for (int i = 0; i < r.pixels.length; i++) tile.alpha[i] = (byte) (r.pixels[i] >>> 24);
      uploadNanos += System.nanoTime() - uploadStart;
      uploadCount++;
      while (tiles.size() > 64) {
        var iterator = tiles.entrySet().iterator();
        Tile old = iterator.next().getValue();
        iterator.remove();
        if (old.texture != null) client.canvas.release(old.texture);
      }
    }
  }

  private void save(Path path, ChunkSnapshot s) {
    try {
      Files.createDirectories(path.getParent());
      Path temp = path.resolveSibling(path.getFileName() + ".tmp");
      try (var out =
          new DataOutputStream(new java.util.zip.GZIPOutputStream(Files.newOutputStream(temp)))) {
        out.writeInt(0x54464432);
        out.writeInt(s.chunkX);
        out.writeInt(s.chunkZ);
        out.writeInt(s.maxY);
        out.writeInt(s.floorY);
        for (int i = 0; i < 16; i++) {
          int[] a = s.section(i);
          out.writeBoolean(a != null);
          if (a != null)
            for (int j = 0; j < 4096; j++) {
              out.writeShort(a[j]);
              out.writeByte(
                  s.blockLight(i, j & 15, j >> 8, j >> 4 & 15)
                      | (s.skyLight(i, j & 15, j >> 8, j >> 4 & 15) << 4));
            }
        }
        for (int[] colors :
            new int[][] {s.tints.grass(), s.tints.foliage(), s.tints.dryFoliage(), s.tints.water()})
          for (int i : colors) out.writeInt(i);
      }
      Files.move(temp, path, StandardCopyOption.REPLACE_EXISTING);
      if (path.getParent().equals(folder)) stored.add(ChunkSnapshot.key(s.chunkX, s.chunkZ));
    } catch (IOException e) {
      failure = "Map save: " + e.getMessage();
    }
  }

  private ChunkSnapshot load(Path path) {
    try {
      if (!Files.isRegularFile(path) || Files.size(path) > 1024 * 1024) return null;
      try (var in =
          new DataInputStream(new java.util.zip.GZIPInputStream(Files.newInputStream(path)))) {
        int format = in.readInt();
        if (format != 0x54464431 && format != 0x54464432) return null;
        int x = in.readInt(), z = in.readInt(), top = in.readInt(), floor = in.readInt();
        int[][] states = new int[16][];
        byte[][] block = new byte[16][], sky = new byte[16][];
        for (int i = 0; i < 16; i++)
          if (in.readBoolean()) {
            states[i] = new int[4096];
            block[i] = new byte[4096];
            sky[i] = new byte[4096];
            for (int j = 0; j < 4096; j++) {
              states[i][j] = in.readUnsignedShort();
              int light = in.readUnsignedByte();
              block[i][j] = (byte) (light & 15);
              sky[i][j] = (byte) (light >> 4);
            }
          }
        int[][] colors = new int[4][256];
        for (int i = 0; i < 256; i++) colors[0][i] = in.readInt();
        if (format == 0x54464432) {
          for (int c = 1; c < 4; c++) for (int i = 0; i < 256; i++) colors[c][i] = in.readInt();
        } else {
          colors[1] = colors[2] = colors[0];
          Arrays.fill(colors[3], 0x3F76E4);
        }
        return new ChunkSnapshot(
            x,
            z,
            states,
            block,
            sky,
            top,
            floor,
            new ChunkSnapshot.Tints(colors[0], colors[1], colors[2], colors[3]));
      }
    } catch (IOException e) {
      return null;
    }
  }

  private void clear() {
    for (Cached state : worlds.values())
      for (Tile tile : state.tiles.values())
        if (tile.texture != null) client.canvas.release(tile.texture);
    worlds.clear();
    if (fog != null) fog.close();
    fog = null;
    maskAt = 0;
    epoch++;
    revision = 0;
    chunks.clear();
    captured.clear();
    reading.clear();
    stored.clear();
    restored.clear();
    completed.clear();
    for (Tile tile : tiles.values()) if (tile.texture != null) client.canvas.release(tile.texture);
    tiles.clear();
    looks = null;
  }

  public void close() {
    markers.close();
    closed = true;
    clear();
    worker.shutdownNow();
    disk.shutdown();
    try {
      if (!disk.awaitTermination(2, TimeUnit.SECONDS))
        OrnitheClient.LOG.warn("Map save queue is still draining at shutdown");
    } catch (InterruptedException interrupted) {
      Thread.currentThread().interrupt();
    }
  }
}
