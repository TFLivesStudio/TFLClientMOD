package dev.theredstonee.trsclient.ornithe.dev;

import com.google.gson.JsonObject;
import java.nio.file.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.IntSupplier;
import org.lwjgl.opengl.*;

/**
 * Read-only snapshot of the fixed-pipeline state that the vanilla framebuffer blit recovers
 * right after the HUD stage, on the client thread. Diagnostic only: every read is guarded and a
 * capture never influences rendering outcomes.
 */
public final class GlSnapshot {
  private static volatile boolean enabled;
  private static IntSupplier phaseSupplier;
  private static Path dir;
  private static final AtomicInteger SEQUENCE = new AtomicInteger();
  private static long lastLogNanos;

  private GlSnapshot() {}

  /** Arms captures for the diagnostic run; idempotent. */
  public static void enable(Path output, IntSupplier phase) {
    if (output != null) dir = output;
    if (phase != null) phaseSupplier = phase;
    enabled = true;
  }

  /** Single capture; safe to call every frame while enabled. */
  public static void capture(String tag) {
    if (!enabled) return;
    try {
      dump(tag);
    } catch (Throwable diagnosticOnly) {
      // Losing a snapshot is acceptable; crashing the frame never is.
    }
  }

  private static int phase() {
    IntSupplier supplier = phaseSupplier;
    return supplier == null ? -1 : supplier.getAsInt();
  }

  private static void dump(String tag) {
    JsonObject state = new JsonObject();
    state.addProperty("tag", tag);
    state.addProperty("phase", phase());
    // The per-unit reads below switch GL_ACTIVE_TEXTURE, so the inherited value must survive
    // any read failure: capture it first and restore it unconditionally before leaving.
    int originalActiveTexture = GL11.glGetInteger(GL13.GL_ACTIVE_TEXTURE);
    state.addProperty("activeTexture", originalActiveTexture);
    try {
      readTextureUnits(state);
    } finally {
      GL13.glActiveTexture(originalActiveTexture);
    }
    readPipelineState(state);
    int index = SEQUENCE.incrementAndGet();
    write(state, index);
    long now = System.nanoTime();
    if (now - lastLogNanos > 500_000_000L || index < 4) {
      lastLogNanos = now;
      System.out.println("[TFL gl-snapshot] #" + index);
    }
  }

  private static void readTextureUnits(JsonObject state) {
    for (int unit = 0; unit < 2; unit++) {
      GL13.glActiveTexture(GL13.GL_TEXTURE0 + unit);
      state.addProperty("unit" + unit + ".binding", GL11.glGetInteger(GL11.GL_TEXTURE_BINDING_2D));
      state.addProperty(
          "unit" + unit + ".textureEnabled", GL11.glIsEnabled(GL11.GL_TEXTURE_2D) ? 1 : 0);
      state.addProperty(
          "unit" + unit + ".texEnv",
          GL11.glGetTexEnvi(GL11.GL_TEXTURE_ENV, GL11.GL_TEXTURE_ENV_MODE));
    }
  }

  private static void readPipelineState(JsonObject state) {
    state.addProperty("program", GL11.glGetInteger(GL20.GL_CURRENT_PROGRAM));
    if (GLContext.getCapabilities().OpenGL30) {
      state.addProperty("vao", GL11.glGetInteger(GL30.GL_VERTEX_ARRAY_BINDING));
    }
    state.addProperty("arrayBuffer", GL11.glGetInteger(GL15.GL_ARRAY_BUFFER_BINDING));
    state.addProperty("elementBuffer", GL11.glGetInteger(GL15.GL_ELEMENT_ARRAY_BUFFER_BINDING));
    boolean fog = GL11.glGetBoolean(GL11.GL_FOG);
    state.addProperty("fog", fog ? 1 : 0);
    if (fog) {
      state.addProperty("fogMode", GL11.glGetInteger(GL11.GL_FOG_MODE));
      state.addProperty("fogStart", GL11.glGetFloat(GL11.GL_FOG_START));
      state.addProperty("fogEnd", GL11.glGetFloat(GL11.GL_FOG_END));
    }
    boolean stencil = GL11.glGetBoolean(GL11.GL_STENCIL_TEST);
    state.addProperty("stencil", stencil ? 1 : 0);
    if (stencil) {
      state.addProperty(
          "stencilFunc",
          GL11.glGetInteger(GL11.GL_STENCIL_FUNC)
              + "/"
              + GL11.glGetInteger(GL11.GL_STENCIL_REF)
              + "/"
              + GL11.glGetInteger(GL11.GL_STENCIL_VALUE_MASK));
    }
    state.addProperty("depth", GL11.glIsEnabled(GL11.GL_DEPTH_TEST) ? 1 : 0);
    state.addProperty("depthFunc", GL11.glGetInteger(GL11.GL_DEPTH_FUNC));
    state.addProperty("depthMask", GL11.glGetBoolean(GL11.GL_DEPTH_WRITEMASK) ? 1 : 0);
    state.addProperty(
        "blend", GL11.glIsEnabled(GL11.GL_BLEND) ? 1 : 0);
    state.addProperty(
        "blendFunc",
        GL11.glGetInteger(GL14.GL_BLEND_SRC_RGB)
            + ","
            + GL11.glGetInteger(GL14.GL_BLEND_DST_RGB)
            + ","
            + GL11.glGetInteger(GL14.GL_BLEND_SRC_ALPHA)
            + ","
            + GL11.glGetInteger(GL14.GL_BLEND_DST_ALPHA));
    state.addProperty("alphaTest", GL11.glIsEnabled(GL11.GL_ALPHA_TEST) ? 1 : 0);
    state.addProperty("scissor", GL11.glIsEnabled(GL11.GL_SCISSOR_TEST) ? 1 : 0);
    state.addProperty("vertexArrayClient", GL11.glIsEnabled(GL11.GL_VERTEX_ARRAY) ? 1 : 0);
    state.addProperty("colorArrayClient", GL11.glIsEnabled(GL11.GL_COLOR_ARRAY) ? 1 : 0);
    state.addProperty("texCoordArrayClient", GL11.glIsEnabled(GL11.GL_TEXTURE_COORD_ARRAY) ? 1 : 0);
    state.addProperty(
        "stacks",
        GL11.glGetInteger(GL11.GL_MODELVIEW_STACK_DEPTH)
            + "/"
            + GL11.glGetInteger(GL11.GL_PROJECTION_STACK_DEPTH)
            + "/"
            + GL11.glGetInteger(GL11.GL_TEXTURE_STACK_DEPTH));
  }

  private static void write(JsonObject state, int index) {
    if (dir == null) return;
    try {
      Files.createDirectories(dir);
      Path rotated = dir.resolve("gl-snapshot-" + Math.floorMod(index, 4) + ".json");
      Files.writeString(rotated, new com.google.gson.Gson().toJson(state));
    } catch (Throwable diagnosticOnly) {
      // File access failure must not reach the render loop.
    }
  }
}
