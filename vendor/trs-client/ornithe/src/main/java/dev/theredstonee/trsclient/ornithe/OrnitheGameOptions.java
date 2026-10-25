package dev.theredstonee.trsclient.ornithe;

import dev.theredstonee.trsclient.core.perf.GameOptions;

/** Vanilla option adapter for explicit presets and shared UndoLog. Never touches Argentum files. */
public final class OrnitheGameOptions implements GameOptions {
  private final OrnitheClient c;
  private boolean rebuild;

  public OrnitheGameOptions(OrnitheClient c) {
    this.c = c;
  }

  public int get(Opt opt) {
    var o = c.mc.options;
    return switch (opt) {
      case VSYNC -> o.vsync ? 1 : 0;
      case VIEW_DISTANCE -> o.viewDistance;
      case GRAPHICS -> o.fancyGraphics ? 1 : 0;
      case CLOUDS -> o.cloudRenderMode;
      case PARTICLES -> o.particles;
      case SMOOTH_LIGHTING -> o.ambientOcclusion;
      case FULLSCREEN -> c.mc.isFullscreen() ? 1 : 0;
      case MAX_FPS -> o.fpsLimit;
      default -> NONE;
    };
  }

  public boolean set(Opt opt, int value) {
    if (get(opt) == NONE) return false;
    int v = opt.clamp(value);
    var o = c.mc.options;
    switch (opt) {
      case VSYNC -> {
        o.vsync = v != 0;
        org.lwjgl.opengl.Display.setVSyncEnabled(o.vsync);
      }
      case VIEW_DISTANCE -> {
        o.viewDistance = v;
        rebuild = true;
      }
      case GRAPHICS -> {
        o.fancyGraphics = v != 0;
        rebuild = true;
      }
      case CLOUDS -> o.cloudRenderMode = v;
      case PARTICLES -> o.particles = v;
      case SMOOTH_LIGHTING -> {
        o.ambientOcclusion = v;
        rebuild = true;
      }
      case MAX_FPS -> o.fpsLimit = v;
      default -> {
        return false;
      }
    }
    return true;
  }

  public boolean smoothLightingIsBoolean() {
    return false;
  }

  public void save() {
    c.mc.options.save();
    if (rebuild) {
      rebuild = false;
      if (c.mc.world != null) c.mc.worldRenderer.reload();
    }
  }

  public String renderer() {
    return org.lwjgl.opengl.GL11.glGetString(org.lwjgl.opengl.GL11.GL_RENDERER);
  }

  public String vendor() {
    return org.lwjgl.opengl.GL11.glGetString(org.lwjgl.opengl.GL11.GL_VENDOR);
  }

  public void preset(dev.theredstonee.trsclient.core.perf.BoostPreset preset) {
    int view = get(Opt.VIEW_DISTANCE);
    for (Opt opt : Opt.values()) {
      int old = get(opt), target = preset.target(opt, old, view, false);
      if (target != NONE && target != old) {
        c.modules.perfUndo.rememberOption(opt, old);
        set(opt, target);
      }
    }
    save();
    c.save();
  }

  public void undo() {
    c.modules.perfUndo.undo(this, c.modules.registry);
    save();
    c.save();
  }
}
