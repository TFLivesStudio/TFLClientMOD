package dev.theredstonee.trsclient.ornithe;

import dev.theredstonee.trsclient.core.perf.LowLatency;
import dev.theredstonee.trsclient.ornithe.capability.CapabilityRegistry.State;
import java.lang.invoke.*;

/**
 * Shared bounded queue limiter. All native entry points are resolved once, never searched during
 * render.
 */
public final class OrnitheLatency {
  private final OrnitheClient c;
  public final LowLatency latency;
  private final boolean argentum;
  private MethodHandle poll;
  public long polls;

  public OrnitheLatency(OrnitheClient c) {
    this.c = c;
    argentum = net.fabricmc.loader.api.FabricLoader.getInstance().isModLoaded("argentum");
    latency = new LowLatency(new Gpu());
    try {
      boolean modern =
          net.fabricmc.loader.api.FabricLoader.getInstance().isModLoaded("legacy-lwjgl3");
      var type = Class.forName(modern ? "org.lwjgl.sdl.SDLEvents" : "org.lwjgl.opengl.Display");
      poll =
          MethodHandles.publicLookup()
              .findStatic(
                  type,
                  modern ? "SDL_PumpEvents" : "processMessages",
                  MethodType.methodType(void.class));
    } catch (ReflectiveOperationException e) {
      OrnitheClient.LOG.info("Late event polling unavailable: {}", e.toString());
    }
    latency.latePollSupported = poll != null;
    c.capabilities.connect(
        c.modules.lowLatency,
        State.EXPERIMENTAL,
        argentum
            ? "Late SDL/mouse polling only with Argentum; TFL GPU fences are bypassed until"
                  + " combined frame-pacing benchmarks exist. Mode preference preserved."
            : "Shared bounded GPU queue limiter with cached native entry points and late mouse"
                  + " polling. Disabled by default; no hardware latency benefit is claimed.",
        argentum ? java.util.List.of("latePoll") : java.util.List.of("mode", "latePoll"),
        "OrnitheLatency.frame",
        "LowLatency",
        "GameRendererMixin.frame");
  }

  public void frame() {
    boolean on = c.capabilities.active(c.modules.lowLatency);
    latency.frameStart(on && !argentum ? c.modules.lowLatencyMode.get() : null, false);
    if (on && c.modules.lowLatencyLatePoll.get() && poll != null)
      try {
        poll.invokeExact();
        org.lwjgl.input.Mouse.poll();
        polls++;
      } catch (Throwable e) {
        poll = null;
        latency.latePollSupported = false;
        OrnitheClient.LOG.warn("Late polling disabled", e);
      }
  }

  public void close() {
    latency.frameStart(null, false);
  }

  private static final class Gpu implements LowLatency.Gpu {
    private MethodHandle fence, wait, delete;
    private final Object[] slots = new Object[8];
    private long next = 1;
    private boolean supported;

    Gpu() {
      try {
        boolean modern =
            net.fabricmc.loader.api.FabricLoader.getInstance().isModLoaded("legacy-lwjgl3");
        var caps =
            Class.forName(modern ? "org.lwjgl.opengl.GL" : "org.lwjgl.opengl.GLContext")
                .getMethod("getCapabilities")
                .invoke(null);
        supported =
            caps.getClass().getField("OpenGL32").getBoolean(caps)
                || caps.getClass().getField("GL_ARB_sync").getBoolean(caps);
        var gl = Class.forName("org.lwjgl.opengl.GL32");
        var f = gl.getMethod("glFenceSync", int.class, int.class);
        Class<?> sync = f.getReturnType();
        var lookup = MethodHandles.publicLookup();
        fence =
            lookup.unreflect(f).asType(MethodType.methodType(Object.class, int.class, int.class));
        wait =
            lookup
                .unreflect(gl.getMethod("glClientWaitSync", sync, int.class, long.class))
                .asType(MethodType.methodType(int.class, Object.class, int.class, long.class));
        delete =
            lookup
                .unreflect(gl.getMethod("glDeleteSync", sync))
                .asType(MethodType.methodType(void.class, Object.class));
      } catch (ReflectiveOperationException e) {
        supported = false;
      }
    }

    public boolean supported() {
      return supported;
    }

    public long fence() {
      try {
        long id = next++;
        int slot = (int) (id % slots.length);
        if (slots[slot] != null) delete.invokeExact(slots[slot]);
        Object result = (Object) fence.invokeExact(0x9117, 0);
        if (result == null || result instanceof Long l && l == 0) return 0;
        slots[slot] = result;
        return id;
      } catch (Throwable e) {
        supported = false;
        return 0;
      }
    }

    private int wait(long id, int flags, long timeout) {
      Object sync = slots[(int) (id % slots.length)];
      if (sync == null) return 0x911A;
      try {
        return (int) wait.invokeExact(sync, flags, timeout);
      } catch (Throwable e) {
        supported = false;
        return 0;
      }
    }

    public boolean await(long id, long timeout) {
      int result = wait(id, 1, timeout);
      return result == 0x911A || result == 0x911C;
    }

    public boolean signaled(long id) {
      int result = wait(id, 0, 0);
      return result == 0x911A || result == 0x911C;
    }

    public void delete(long id) {
      int slot = (int) (id % slots.length);
      Object sync = slots[slot];
      slots[slot] = null;
      if (sync != null)
        try {
          delete.invokeExact(sync);
        } catch (Throwable e) {
          supported = false;
        }
    }
  }
}
