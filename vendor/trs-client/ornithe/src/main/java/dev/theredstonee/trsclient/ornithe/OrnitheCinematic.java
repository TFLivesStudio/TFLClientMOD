package dev.theredstonee.trsclient.ornithe;

import dev.theredstonee.trsclient.core.cinematic.*;
import dev.theredstonee.trsclient.core.module.KeySetting;
import dev.theredstonee.trsclient.ornithe.capability.CapabilityRegistry.State;
import net.minecraft.client.options.KeyBinding;
import net.minecraft.entity.Entity;

/**
 * Detached local camera; the player/network entity stays at its original position and orientation.
 */
public final class OrnitheCinematic implements Cinematic.Host {
  private final OrnitheClient c;
  public final Cinematic controller;
  private final boolean[] held = new boolean[5];
  private Entity before, camera;
  private int perspective = -1;
  public CameraPose pose;

  public OrnitheCinematic(OrnitheClient c) {
    this.c = c;
    controller =
        Cinematic.init(
            c.modules.cinematic, net.fabricmc.loader.api.FabricLoader.getInstance().getConfigDir());
    var settings =
        new java.util.ArrayList<>(
            c.modules.cinematic.cinematic.settings().stream().map(s -> s.key()).toList());
    settings.remove("record");
    c.capabilities.connect(
        c.modules.cinematic.cinematic,
        State.EXPERIMENTAL,
        "Shared offline keyframes/playback and collision-limited local freecam. Received chunks"
            + " only. External recording unavailable; multiplayer gameplay acceptance remains"
            + " pending.",
        settings,
        "OrnitheCinematic",
        "Cinematic.Host",
        "Minecraft.setCamera",
        "CinematicInputMixin",
        "GameRendererMixin");
  }

  public boolean detached() {
    return camera != null && c.mc.getCamera() == camera;
  }

  public void tick() {
    var m = c.modules.cinematic;
    KeySetting[] keys = {m.addKey, m.playKey, m.removeKey, m.clearKey, m.freecamKey};
    for (int i = 0; i < keys.length; i++) {
      boolean down = OrnitheKeys.isDown(keys[i].get());
      if (down && !held[i] && c.mc.screen == null && c.capabilities.active(m.cinematic)) {
        switch (i) {
          case 0 -> controller.addKeyframe(this);
          case 1 -> controller.togglePlay(this);
          case 2 -> controller.removeLast(this);
          case 3 -> controller.clear(this);
          case 4 -> controller.toggleFreecam(this);
        }
      }
      held[i] = down;
    }
    controller.tick(this, c.mc.screen != null);
    if (c.mc.player == null || c.mc.player.getHealth() <= 0) {
      controller.stop(this);
      if (controller.freecam()) controller.toggleFreecam(this);
      detachView(false);
    }
  }

  public void frame() {
    pose = controller.frame(this);
    if (pose != null && camera != null) {
      camera.setPosition(pose.x, pose.y, pose.z);
      camera.lastX = camera.prevX = pose.x;
      camera.lastY = camera.prevY = pose.y;
      camera.lastZ = camera.prevZ = pose.z;
      camera.yaw = camera.lastYaw = pose.yaw;
      camera.pitch = camera.lastPitch = pose.pitch;
    }
  }

  public String worldKey() {
    return c.mapPlatform.worldKey();
  }

  public String dimension() {
    return c.mapPlatform.dimension();
  }

  public boolean camera(CameraPose out) {
    if (c.mc.player == null) return false;
    if (pose != null && detached()) {
      out.set(pose.x, pose.y, pose.z, pose.yaw, pose.pitch, pose.fov);
      return true;
    }
    var e = c.mc.getCamera();
    if (e == null) e = c.mc.player;
    out.set(e.x, e.y + e.getEyeHeight(), e.z, e.yaw, e.pitch, c.mc.options.fov);
    return true;
  }

  public void actionBar(String text) {
    c.mapPlatform.message(text);
  }

  public boolean hudHidden() {
    return c.mc.options.hideGui;
  }

  public void setHudHidden(boolean hidden) {
    c.mc.options.hideGui = hidden;
  }

  public boolean recordingAvailable() {
    return false;
  }

  public boolean recording() {
    return false;
  }

  public void toggleRecording() {}

  public void particle(boolean keyframe, double x, double y, double z) {
    if (c.mc.world != null)
      c.mc.particleManager.addParticle(
          net.minecraft.entity.particle.ParticleType.REDSTONE.getId(),
          x,
          y,
          z,
          keyframe ? 1 : .2,
          0,
          keyframe ? 0 : 1);
  }

  public boolean solid(int x, int y, int z) {
    if (!c.redstone.world.loaded(x, y, z)) return true;
    var state = c.mc.world.getBlockState(new net.minecraft.util.math.BlockPos(x, y, z));
    var material = state.getBlock().getMaterial();
    return state.getBlock().isOpaque() && material.blocksMovement();
  }

  private boolean down(KeyBinding key) {
    return OrnitheKeys.isDown(OrnitheKeys.name(key.getKeyCode()));
  }

  public void freecamInput(Freecam.Input input) {
    if (c.mc.screen != null) return;
    var o = c.mc.options;
    input.forward = down(o.forwardKey);
    input.back = down(o.backKey);
    input.left = down(o.leftKey);
    input.right = down(o.rightKey);
    input.up = down(o.jumpKey);
    input.down = down(o.sneakKey);
    input.boost = down(o.sprintKey);
  }

  public String serverAddress() {
    var s = c.mc.getCurrentServerEntry();
    return s == null ? null : s.ip;
  }

  public String serverMotd() {
    return c.mapPlatform.serverMotd();
  }

  public void detachView(boolean detached) {
    if (detached && camera == null && c.mc.player != null) {
      c.freelook.stop();
      c.emotes.attack();
      before = c.mc.getCamera();
      perspective = c.mc.options.perspective;
      c.mc.options.perspective = 0;
      camera =
          new Entity(c.mc.world) {
            protected void registerSyncedData() {}

            protected void readCustomNbt(net.minecraft.nbt.NbtCompound nbt) {}

            protected void writeCustomNbt(net.minecraft.nbt.NbtCompound nbt) {}

            public float getEyeHeight() {
              return 0;
            }
          };
      camera.setPosition(c.mc.player.x, c.mc.player.y + c.mc.player.getEyeHeight(), c.mc.player.z);
      c.mc.setCamera(camera);
    } else if (!detached && camera != null) {
      if (c.mc.getCamera() == camera) c.mc.setCamera(c.mc.player != null ? c.mc.player : before);
      if (c.mc.options.perspective == 0 && perspective >= 0) c.mc.options.perspective = perspective;
      camera = before = null;
      perspective = -1;
      pose = null;
    }
  }

  public void holdView() {
    if (camera != null && camera.world != c.mc.world) detachView(false);
  }
}
