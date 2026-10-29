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
  private Object resourceGeneration = new Object();

  public OrnitheMapPlatform(OrnitheClient c) {
    this.c = c;
  }

  public void updateIdentity() {
    if (identity != c.mc.world || (inWorld() && worldKey.isEmpty())) {
      identity = c.mc.world;
      chunk = null;
      dimension = inWorld() ? "dim" + c.mc.world.dimension.getId() : "";
      if (!inWorld()) worldKey = "";
      else if (c.mc.isSingleplayer() && c.mc.getServer() != null)
        worldKey = WaypointStore.singleplayerKey(c.mc.getServer().getWorldSaveName());
      else {
        var s = c.mc.getCurrentServerEntry();
        worldKey = s == null ? "" : WaypointStore.serverKey(s.ip);
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
      out.kind=net.minecraft.entity.Entities.getKey(entity);
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
    saveWaypoints = true;
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
