package dev.theredstonee.trsclient.ornithe;

import dev.theredstonee.trsclient.core.panorama.*;
import dev.theredstonee.trsclient.core.ui.menu.ModulePanel;
import dev.theredstonee.trsclient.ornithe.capability.CapabilityRegistry.State;

/**
 * Six native world frames, stitched/encoded by the existing asynchronous common panorama worker.
 */
public final class OrnithePanorama {
  private final OrnitheClient c;
  public Panorama.OwnCapture capture;
  private boolean hiddenBefore;
  private Object world;
  private java.nio.IntBuffer pixels;
  private int[] frame;
  public long faces;

  public OrnithePanorama(OrnitheClient c) {
    this.c = c;
    Panorama.get().setGameDir(c.mc.gameDir.toPath());
    ModulePanel.Registry.set(
        c.modules.comfort.panorama, new PanoramaPanel(c.modules.comfort, () -> true));
    c.capabilities.connect(
        c.modules.comfort.panorama,
        State.EXPERIMENTAL,
        "Shared offline cube/equirectangular capture using six native world frames and asynchronous"
            + " encoding. Temporarily detaches camera/hides HUD, restoring both. No launcher"
            + " sharing.",
        java.util.List.of("format"),
        "OrnithePanorama",
        "Panorama.OwnCapture",
        "GameRendererMixin.renderWorld",
        "PanoramaPanel");
  }

  public void tick() {
    if (!c.capabilities.active(c.modules.comfort.panorama)) {
      Panorama.get().cancelRequest();
    }
    if (capture != null
        && (!c.capabilities.active(c.modules.comfort.panorama)
            || world != c.mc.world
            || c.mc.player == null
            || c.mc.screen != null)) {
      capture.cancel("Panorama cancelled: world, screen or module changed");
      restore();
    }
    int action =
        Panorama.get()
            .poll(System.currentTimeMillis(), c.mapPlatform.inWorld(), c.mc.screen != null);
    if (action == Panorama.POLL_CLOSE_SCREEN) c.mc.openScreen(null);
    if (action == Panorama.POLL_CAPTURE
        && c.capabilities.active(c.modules.comfort.panorama)
        && capture == null) {
      c.cinematic.controller.stop(c.cinematic);
      if (c.cinematic.controller.freecam()) c.cinematic.controller.toggleFreecam(c.cinematic);
      capture =
          Panorama.get()
              .beginOwn(c.mc.gameDir.toPath(), c.mc.player.yaw, System.currentTimeMillis());
      if (capture != null) {
        world = c.mc.world;
        hiddenBefore = c.mc.options.hideGui;
        c.mc.options.hideGui = true;
        c.cinematic.detachView(true);
      }
    }
  }

  public void frame() {
    if (capture == null) return;
    var camera = c.mc.getCamera();
    if (camera == null) return;
    camera.yaw = camera.lastYaw = capture.yaw();
    camera.pitch = camera.lastPitch = capture.pitch();
  }

  public float fov() {
    return c.mc.width >= c.mc.height
        ? 90f
        : (float) Math.toDegrees(2 * Math.atan((double) c.mc.height / Math.max(1, c.mc.width)));
  }

  public void afterWorld() {
    if (capture == null || !capture.wantsFrame()) return;
    try {
      int count = Math.multiplyExact(c.mc.width, c.mc.height);
      if (pixels == null || pixels.capacity() != count) {
        pixels = org.lwjgl.BufferUtils.createIntBuffer(count);
        frame = new int[count];
      }
      pixels.clear();
      org.lwjgl.opengl.GL11.glReadPixels(
          0,
          0,
          c.mc.width,
          c.mc.height,
          org.lwjgl.opengl.GL12.GL_BGRA,
          org.lwjgl.opengl.GL12.GL_UNSIGNED_INT_8_8_8_8_REV,
          pixels);
      for (int y = 0; y < c.mc.height; y++)
        for (int x = 0; x < c.mc.width; x++)
          frame[y * c.mc.width + x] = pixels.get((c.mc.height - 1 - y) * c.mc.width + x);
      capture.frame(frame, c.mc.width, c.mc.height);
      faces++;
      if (capture.finished()) restore();
    } catch (RuntimeException error) {
      capture.cancel(error.toString());
      restore();
      OrnitheClient.LOG.warn("Panorama capture failed", error);
    }
  }

  private void restore() {
    capture = null;
    pixels = null;
    frame = null;
    c.mc.options.hideGui = hiddenBefore;
    c.cinematic.detachView(false);
  }
}
