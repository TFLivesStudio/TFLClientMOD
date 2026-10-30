package dev.theredstonee.trsclient.ornithe.dev;

import dev.diorama.data.ChunkSnapshot;
import dev.theredstonee.trsclient.ornithe.*;
import java.util.*;
import net.minecraft.world.chunk.*;

/** Native fixtures for the reported nullable light buffers and same-dimension session isolation. */
final class ServerMapRegression {
  static Map<String, Object> run(OrnitheClient c) throws Exception {
    var method =
        OrnitheDiorama.class.getDeclaredMethod("capture", WorldChunk.class, int.class, int.class);
    method.setAccessible(true);
    var chunk = new WorldChunk(c.mc.world, 0, 0);
    var section = new WorldChunkSection(0, true);
    section.setBlockLightStorage(null);
    section.setSkyLightStorage(null);
    chunk.getSections()[0] = section;
    ChunkSnapshot safe = (ChunkSnapshot) method.invoke(c.diorama, chunk, 0, 0);
    if (safe == null || safe.blockLight(0, 0, 0, 0) != 0 || safe.skyLight(0, 0, 0, 0) != 15)
      throw new AssertionError("Missing light buffers must have stable defaults");
    section.setBlockStates(null);
    if (method.invoke(c.diorama, chunk, 0, 0) != null)
      throw new AssertionError("Incomplete block sections must be deferred");
    var worldField = OrnitheMapPlatform.class.getDeclaredField("worldKey");
    worldField.setAccessible(true);
    String original = (String) worldField.get(c.mapPlatform);
    Object world = c.mc.world;
    String dimension = c.mapPlatform.dimension();
    var chunksField = OrnitheDiorama.class.getDeclaredField("chunks");
    chunksField.setAccessible(true);
    Object originalChunks = chunksField.get(c.diorama);
    try {
      worldField.set(c.mapPlatform, original + "/test-remote-B");
      c.diorama.tick();
      Object otherChunks = chunksField.get(c.diorama);
      if (originalChunks == otherChunks)
        throw new AssertionError(
            "Worlds with the same dimension must not share mutable chunk storage");
      worldField.set(c.mapPlatform, original);
      c.diorama.tick();
      if (originalChunks != chunksField.get(c.diorama)
          || world != c.mc.world
          || !dimension.equals(c.mapPlatform.dimension()))
        throw new AssertionError("Returning to same-dimension A must restore A cache");
    } finally {
      worldField.set(c.mapPlatform, original);
      c.diorama.tick();
    }
    var epochField = OrnitheDiorama.class.getDeclaredField("epoch");
    epochField.setAccessible(true);
    long currentEpoch = epochField.getLong(c.diorama);
    var loadedType = Class.forName("dev.theredstonee.trsclient.ornithe.OrnitheDiorama$Loaded");
    var loadedConstructor = loadedType.getDeclaredConstructors()[0];
    loadedConstructor.setAccessible(true);
    var queueField = OrnitheDiorama.class.getDeclaredField("restored");
    queueField.setAccessible(true);
    int[] tint = new int[256];
    var lateSnapshot =
        new ChunkSnapshot(
            1000000,
            1000000,
            new int[16][],
            new byte[16][],
            new byte[16][],
            0,
            0,
            new ChunkSnapshot.Tints(tint, tint, tint, tint));
    ((java.util.Queue<Object>) queueField.get(c.diorama))
        .add(loadedConstructor.newInstance(currentEpoch - 1, lateSnapshot));
    var captureClock = OrnitheDiorama.class.getDeclaredField("lastCapture");
    captureClock.setAccessible(true);
    long oldClock = captureClock.getLong(c.diorama);
    captureClock.setLong(c.diorama, System.nanoTime() + 1_000_000_000L);
    try {
      c.diorama.tick();
    } finally {
      captureClock.setLong(c.diorama, oldClock);
    }
    if (((java.util.Map<?, ?>) chunksField.get(c.diorama))
        .containsKey(ChunkSnapshot.key(1000000, 1000000)))
      throw new AssertionError("A stale disk load crossed a world boundary");
    // A player-type NPC with no tab-list entry still supplies its native skin texture.
    var npc =
        new net.minecraft.client.entity.living.player.RemoteClientPlayerEntity(
            c.mc.world,
            new com.mojang.authlib.GameProfile(
                java.util.UUID.fromString("fbc8b872-9fd0-4ffd-949b-70937ef56700"), "TFL Test NPC"));
    npc.setPosition(c.mc.player.x + 3, c.mc.player.y, c.mc.player.z + 3);
    c.mc.world.forceEntity(-9150, npc);
    boolean[] face = {false};
    // MapEntity is final, so inspect the sink's filled records after enumeration.
    var entities = new ArrayList<dev.theredstonee.trsclient.core.map.MapEntity>();
    c.mapPlatform.entities(
        () -> {
          var item = new dev.theredstonee.trsclient.core.map.MapEntity();
          entities.add(item);
          return item;
        },
        16,
        false);
    for (var item : entities)
      if (npc.getUuid().equals(item.uuid) && item.skin != null) face[0] = true;
    if (!face[0])
      throw new AssertionError("NPC native face must survive a missing player-list entry");
    var waypoint =
        new dev.theredstonee.trsclient.core.waypoint.Waypoint(
            "Home",
            (int) c.mapPlatform.x() + 7,
            (int) c.mapPlatform.y(),
            (int) c.mapPlatform.z() + 3,
            c.mapPlatform.dimension(),
            0x0090ff);
    waypoint.id = java.util.UUID.fromString("16d2fc8e-ce75-47ef-9311-e2821f4cc68d");
    waypoint.icon = "diorama:home";
    waypoint.folder = "Test folder";
    waypoint.temporary = true;
    if (c.waypoints.all(c.mapPlatform.worldKey()).stream().noneMatch(p -> waypoint.id.equals(p.id)))
      c.waypoints.add(c.mapPlatform.worldKey(), waypoint);
    return Map.of(
        "staleDiskLoadRejected",
        true,
        "nullLightBuffersSafe",
        true,
        "incompleteSectionDeferred",
        true,
        "sameDimensionCachesIsolated",
        true,
        "returningWorldRestoresMemoryCache",
        true,
        "npcWithoutTabEntryHasFace",
        true);
  }
}
