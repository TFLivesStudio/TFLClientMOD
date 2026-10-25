package dev.theredstonee.trsclient.ornithe;

import dev.theredstonee.trsclient.core.ui.BorderlessState;
import dev.theredstonee.trsclient.ornithe.capability.CapabilityRegistry.State;
import java.lang.reflect.Method;

/** Cached SDL bridge, used only on fullscreen transitions. Never destroys the OpenGL context. */
public final class OrnitheBorderless {
  private final OrnitheClient c;
  private Method handle, bordered, fullscreen, position, size, getPosition;
  private final java.nio.IntBuffer xBuffer = org.lwjgl.BufferUtils.createIntBuffer(1),
      yBuffer = org.lwjgl.BufferUtils.createIntBuffer(1);
  private int x, y, width = 854, height = 480;
  private boolean active;

  public OrnitheBorderless(OrnitheClient c) {
    this.c = c;
    if (net.fabricmc.loader.api.FabricLoader.getInstance().isModLoaded("legacy-lwjgl3"))
      try {
        var display = Class.forName("org.lwjgl.opengl.Display");
        var sdl = Class.forName("org.lwjgl.sdl.SDLVideo");
        handle = display.getMethod("getHandle");
        bordered = sdl.getMethod("SDL_SetWindowBordered", long.class, boolean.class);
        fullscreen = sdl.getMethod("SDL_SetWindowFullscreen", long.class, boolean.class);
        position = sdl.getMethod("SDL_SetWindowPosition", long.class, int.class, int.class);
        size = sdl.getMethod("SDL_SetWindowSize", long.class, int.class, int.class);
        getPosition =
            sdl.getMethod(
                "SDL_GetWindowPosition",
                long.class,
                java.nio.IntBuffer.class,
                java.nio.IntBuffer.class);
      } catch (ReflectiveOperationException e) {
        handle = null;
        OrnitheClient.LOG.warn("SDL borderless bridge unavailable", e);
      }
    if (handle == null)
      c.capabilities.declare(
          "borderlessFullscreen",
          State.OPTIONAL_INTEGRATION,
          "Runtime borderless requires the Legacy LWJGL3 SDL bridge. Baseline keeps Vanilla"
              + " fullscreen without recreating its GL context.");
    else
      c.capabilities.connect(
          c.modules.borderlessFullscreen,
          State.EXPERIMENTAL,
          "SDL borderless on the primary desktop, with window geometry restoration and no GL"
              + " context recreation. Multi-monitor/Windows acceptance remains pending.",
          java.util.List.of(),
          "OrnitheBorderless",
          "MinecraftMixin.toggleFullscreen",
          "SDLVideo");
  }

  private long window() throws ReflectiveOperationException {
    return (long) handle.invoke(null);
  }

  public void beforeToggle() {
    if (handle == null || c.mc.isFullscreen()) return;
    try {
      long w = window();
      getPosition.invoke(null, w, xBuffer, yBuffer);
      x = xBuffer.get(0);
      y = yBuffer.get(0);
      width = c.mc.width;
      height = c.mc.height;
    } catch (ReflectiveOperationException e) {
      OrnitheClient.LOG.warn("Could not capture window geometry", e);
    }
  }

  public boolean apply(boolean flag) {
    if (handle == null || (!active && !c.capabilities.active(c.modules.borderlessFullscreen)))
      return false;
    try {
      long w = window();
      fullscreen.invoke(null, w, false);
      if (flag) {
        bordered.invoke(null, w, false);
        var mode = org.lwjgl.opengl.Display.getDesktopDisplayMode();
        position.invoke(null, w, 0, 0);
        size.invoke(null, w, mode.getWidth(), mode.getHeight());
        active = true;
      } else {
        bordered.invoke(null, w, true);
        size.invoke(null, w, width, height);
        position.invoke(null, w, x, y);
        active = false;
      }
      BorderlessState.mark(active);
      return true;
    } catch (ReflectiveOperationException e) {
      OrnitheClient.LOG.warn("Borderless transition failed", e);
      active = false;
      BorderlessState.mark(false);
      return false;
    }
  }

  public void frame() {
    if (active && !c.capabilities.active(c.modules.borderlessFullscreen))
      try {
        bordered.invoke(null, window(), true);
        org.lwjgl.opengl.Display.setFullscreen(c.mc.isFullscreen());
        active = false;
        BorderlessState.mark(false);
      } catch (Exception e) {
        OrnitheClient.LOG.warn("Fullscreen restoration failed", e);
      }
  }
}
