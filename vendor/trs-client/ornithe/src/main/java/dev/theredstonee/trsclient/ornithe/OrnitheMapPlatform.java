package dev.theredstonee.trsclient.ornithe;

import dev.theredstonee.trsclient.core.map.*;
import dev.theredstonee.trsclient.core.waypoint.WaypointStore;
import java.io.*;
import net.minecraft.block.material.Material;
import net.minecraft.entity.living.LivingEntity;
import net.minecraft.entity.living.player.PlayerEntity;
import net.minecraft.resource.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.LightType;
import net.minecraft.world.chunk.WorldChunk;

/**
 * Bounded reads from received client chunks only. No server requests or integrated-server world
 * access.
 */
public final class OrnitheMapPlatform implements MapPlatform, ChunkReader {
  private final OrnitheClient c;
  private WorldChunk chunk;
  private final BlockPos.Mutable position = new BlockPos.Mutable();
  private int cx, cz;
  private String worldKey = "", dimension = "";
  private Object identity;
  private boolean saveWaypoints;
  private final java.util.Set<String> pendingWaypointWorlds = new java.util.LinkedHashSet<>();
  private String importedWaypoints = "";

  public java.nio.file.Path waypointFile(String key) {
    return c.mc
        .gameDir
        .toPath()
        .resolve("tfl-maps/waypoints")
        .resolve(
            java.util
                .UUID
                .nameUUIDFromBytes(key.getBytes(java.nio.charset.StandardCharsets.UTF_8))
                .toString())
        .resolve("waypoints.json");
  }

  public void waypointsChanged(String key) {
    pendingWaypointWorlds.add(key);
    saveWaypoints = true;
  }

  private dev.theredstonee.trsclient.core.map.session.WorldCatalog catalog;
  private String providedWorldId;
  private Object sessionWorld, sessionContext = new Object();

  public dev.theredstonee.trsclient.core.map.session.WorldCatalog catalog() {
    if (catalog == null)
      try {
        catalog =
            new dev.theredstonee.trsclient.core.map.session.WorldCatalog(
                c.mc.gameDir.toPath().resolve("tfl-maps/worlds.json"));
      } catch (IOException e) {
        throw new IllegalStateException("Cannot open map world catalog", e);
      }
    return catalog;
  }

  public void respawning() {
    identity = null;
    sessionWorld = c.mc.world;
    sessionContext = new Object();
    providedWorldId = null;
    chunk = null;
  }

  public void worldId(int id) {
    if (sessionWorld != c.mc.world) {
      sessionWorld = c.mc.world;
      sessionContext = new Object();
    }
    providedWorldId = Integer.toHexString(id);
    identity = null;
  }

  public void selectedWorld() {
    try {
      worldKey = catalog().backendKey(catalog().namespace());
    } catch (IOException e) {
      OrnitheClient.LOG.error("Cannot select cached world", e);
    }
    chunk = null;
  }

  private static final class Folders {
    java.util.List<String> custom = new java.util.ArrayList<>(),
        hidden = new java.util.ArrayList<>();
  }

  private Folders folders = new Folders();
  private String folderOwner = "";

  private Folders readFolders(String owner) {
    var file = waypointFile(owner).resolveSibling("folders.json");
    if (java.nio.file.Files.isRegularFile(file))
      try (var reader = java.nio.file.Files.newBufferedReader(file)) {
        Folders read = new com.google.gson.Gson().fromJson(reader, Folders.class);
        if (read != null && read.custom != null && read.hidden != null) return read;
      } catch (Exception e) {
        OrnitheClient.LOG.warn("Cannot read waypoint folders", e);
      }
    return new Folders();
  }

  private void writeFolders(String owner, Folders value) throws IOException {
    var file = waypointFile(owner).resolveSibling("folders.json");
    java.nio.file.Files.createDirectories(file.getParent());
    var temp = file.resolveSibling("folders.json.tmp");
    try (var out = java.nio.file.Files.newBufferedWriter(temp)) {
      new com.google.gson.Gson().toJson(value, out);
    }
    java.nio.file.Files.move(temp, file, java.nio.file.StandardCopyOption.REPLACE_EXISTING);
    if (owner.equals(folderOwner)) folders = value;
  }

  private void saveWaypointWorld(String owner) throws IOException {
    c.waypoints.exportDiorama(waypointFile(owner), owner);
    Folders value = readFolders(owner);
    for (var point : c.waypoints.all(owner))
      if (point.folder != null && !point.folder.isEmpty() && !value.custom.contains(point.folder))
        value.custom.add(point.folder);
    writeFolders(owner, value);
  }

  public boolean hiddenFolder(String name) {
    return hiddenFolder(worldKey, name);
  }

  public boolean hiddenFolder(String owner, String name) {
    if (!folderOwner.equals(owner)) {
      folderOwner = owner;
      folders = readFolders(owner);
    }
    return name != null && !name.isEmpty() && folders.hidden.contains("folder:" + name);
  }

  public void toggleFolder(String name) {
    toggleFolder(worldKey, name);
  }

  public void toggleFolder(String owner, String name) {
    Folders value = readFolders(owner);
    String key = "folder:" + name;
    if (!value.hidden.remove(key)) value.hidden.add(key);
    if (!value.custom.contains(name)) value.custom.add(name);
    try {
      writeFolders(owner, value);
    } catch (IOException e) {
      OrnitheClient.LOG.error("Cannot save waypoint folders", e);
    }
  }

  private Object resourceGeneration = new Object();

  public OrnitheMapPlatform(OrnitheClient c) {
    this.c = c;
  }

  public void updateIdentity() {
    if (saveWaypoints) {
      saveWaypoints = false;
      for (String key : pendingWaypointWorlds)
        try {
          saveWaypointWorld(key);
        } catch (IOException e) {
          OrnitheClient.LOG.error("Cannot persist Diorama waypoints", e);
        }
      pendingWaypointWorlds.clear();
      try {
        c.waypoints.saveIfDirty();
      } catch (IOException e) {
        OrnitheClient.LOG.error("Cannot persist waypoint migration", e);
      }
    }
    if (sessionWorld != c.mc.world) {
      sessionWorld = c.mc.world;
      sessionContext = new Object();
      providedWorldId = null;
    }
    if (identity != c.mc.world || (inWorld() && worldKey.isEmpty())) {
      identity = c.mc.world;
      chunk = null;
      dimension = inWorld() ? "dim" + c.mc.world.dimension.getId() : "";
      if (!inWorld()) {
        worldKey = "";
        providedWorldId = null;
        try {
          catalog().begin(null, "", null);
        } catch (IOException ignored) {
        }
      } else if (c.mc.isSingleplayer() && c.mc.getServer() != null) {
        worldKey = WaypointStore.singleplayerKey(c.mc.getServer().getWorldSaveName());
        try {
          catalog().begin(sessionContext, worldKey, "local");
        } catch (IOException e) {
          OrnitheClient.LOG.error("Map camera preferences", e);
        }
      } else {
        var s = c.mc.getCurrentServerEntry();
        String origin = s == null ? "server:unknown" : WaypointStore.serverKey(s.ip);
        try {
          worldKey = catalog().backendKey(catalog().begin(sessionContext, origin, providedWorldId));
          String oldKey = origin + "/" + dimension;
          var oldFolder =
              c.mc
                  .gameDir
                  .toPath()
                  .resolve("tfl-maps")
                  .resolve(
                      java.util
                          .UUID
                          .nameUUIDFromBytes(
                              oldKey.getBytes(java.nio.charset.StandardCharsets.UTF_8))
                          .toString());
          if (java.nio.file.Files.isDirectory(oldFolder) || !c.waypoints.all(origin).isEmpty())
            catalog()
                .addExisting(
                    "legacy-address",
                    dev.theredstonee.trsclient.core.i18n.I18n.tr("serverMaps.previous"),
                    origin);
        } catch (IOException e) {
          OrnitheClient.LOG.error("Cannot persist map world identity", e);
          worldKey = origin + "/unsaved/" + java.util.UUID.randomUUID();
        }
      }
    }
    if (!worldKey.isEmpty() && !worldKey.equals(importedWaypoints)) {
      try {
        c.waypoints.importDiorama(waypointFile(worldKey), worldKey);
        importedWaypoints = worldKey;
      } catch (IOException e) {
        OrnitheClient.LOG.error("Cannot import Diorama waypoints", e);
      }
    }
    if (saveWaypoints) {
      saveWaypoints = false;
      try {
        c.waypoints.saveIfDirty();
      } catch (IOException e) {
        OrnitheClient.LOG.error("Could not persist waypoints", e);
      }
    }
  }

  public void reloaded() {
    resourceGeneration = new Object();
    chunk = null;
  }

  public boolean inWorld() {
    return c.mc != null && c.mc.world != null && c.mc.player != null;
  }

  public String worldKey() {
    return worldKey;
  }

  public String dimension() {
    return dimension;
  }

  public double x() {
    return inWorld() ? c.mc.player.x : 0;
  }

  public double y() {
    return inWorld() ? c.mc.player.y : 0;
  }

  public double z() {
    return inWorld() ? c.mc.player.z : 0;
  }

  public float yaw() {
    return inWorld() ? c.mc.player.yaw : 0;
  }

  public ChunkReader reader() {
    return this;
  }

  public int renderDistance() {
    return c.mc.options.viewDistance;
  }

  public int skyLight() {
    return inWorld() ? c.mc.world.getLight(LightType.SKY, new BlockPos(x(), y() + 1.6, z())) : 15;
  }

  public void entities(EntitySink sink, double radius, boolean lineOfSightOnly) {
    if (!inWorld()) return;
    int checks = 0;
    for (var entity : c.mc.world.entities) {
      if (entity instanceof net.minecraft.entity.living.ArmorStandEntity
          || entity == c.mc.player
          || !(entity instanceof LivingEntity living)
          || living.getHealth() <= 0) continue;
      double dx = entity.x - x(), dz = entity.z - z();
      if (dx * dx + dz * dz > radius * radius) continue;
      if (lineOfSightOnly && (checks++ >= 48 || !c.mc.player.canSee(entity))) continue;
      var out = sink.add();
      if (out == null) return;
      // Only received, living entities. Unknown mob taxonomy uses the neutral marker.
      out.set(
          entity instanceof PlayerEntity
              ? MapEntity.PLAYER
              : entity instanceof net.minecraft.entity.living.mob.monster.Monster
                  ? MapEntity.HOSTILE
                  : MapEntity.PASSIVE,
          entity.prevX,
          entity.prevZ,
          entity.x,
          entity.y,
          entity.z,
          entity.yaw);
      out.uuid = entity.getUuid();
      out.name = entity.getName();
      out.kind = net.minecraft.entity.Entities.getKey(entity);
      if (entity instanceof net.minecraft.client.entity.living.player.ClientPlayerEntity player)
        out.skin = c.canvas.game(player.getSkinTextureLocation().toString(), 64, 64);
    }
  }

  public String biome() {
    return inWorld() ? c.mc.world.getBiome(new BlockPos(x(), y(), z())).name : "";
  }

  public long dayTime() {
    return inWorld() ? c.mc.world.getTimeOfDay() : -1;
  }

  public double guiScale() {
    return c.window().getScale();
  }

  public WaypointStore waypoints() {
    return c.waypoints;
  }

  public String waypointWorldKey() {
    return worldKey;
  }

  public void waypointsChanged() {
    waypointsChanged(worldKey);
  }

  public String serverMotd() {
    var s = c.mc.getCurrentServerEntry();
    return s == null ? null : s.motd;
  }

  public boolean openWorldMap() {
    if (!inWorld()) return false;
    c.openWorldMap(c.mc.screen);
    return true;
  }

  public int movement() {
    return inWorld() && c.mc.player.isSprinting() ? SPRINTING : 0;
  }

  public byte[] readResource(String ns, String path) throws IOException {
    try (InputStream in =
        c.mc.getResourceManager().getResource(new Identifier(ns + ":" + path)).asStream()) {
      return in.readAllBytes();
    }
  }

  public Object resourceGeneration() {
    return resourceGeneration;
  }

  public void message(String text) {
    if (c.mc.player != null) c.mc.player.sendMessage(new net.minecraft.text.LiteralText(text));
  }

  public boolean isLoaded(int x, int z) {
    return inWorld()
        && !(c.mc.world.getChunkSource().getChunk(x, z)
            instanceof net.minecraft.world.chunk.EmptyChunk);
  }

  public boolean open(int x, int z) {
    if (!isLoaded(x, z)) {
      chunk = null;
      return false;
    }
    chunk = c.mc.world.getChunkSource().getChunk(x, z);
    cx = x;
    cz = z;
    return chunk != null && !chunk.isEmpty();
  }

  public int minY() {
    return 0;
  }

  public int top(int x, int z) {
    return chunk == null ? 0 : chunk.getHeight(x, z);
  }

  public int block(int x, int y, int z) {
    if (chunk == null || y < 0 || y > 255) return AIR;
    var state = chunk.getBlockState(position.set(x, y, z));
    var block = state.getBlock();
    if (block.getMaterial() == Material.AIR) return AIR;
    return (block.getMapColor(state).color & 0xFFFFFF) | (block.isOpaque() ? OPAQUE : 0);
  }

  public int tint(int x, int y, int z) {
    if (chunk == null) return -1;
    BlockPos pos = new BlockPos((cx << 4) + x, y, (cz << 4) + z);
    return chunk.getBlockState(pos).getBlock().getColorTint(c.mc.world, pos);
  }

  public boolean sectionEmpty(int y) {
    return chunk == null || y < 0 || y > 255 || chunk.isEmpty(y & ~15, (y & ~15) + 15);
  }
}
