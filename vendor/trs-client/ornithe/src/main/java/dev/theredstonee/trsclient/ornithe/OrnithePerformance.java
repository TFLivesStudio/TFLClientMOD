package dev.theredstonee.trsclient.ornithe;

import dev.theredstonee.trsclient.core.perf.DynamicFps;
import dev.theredstonee.trsclient.ornithe.capability.CapabilityRegistry.State;
import java.util.List;
import org.lwjgl.input.*;
import org.lwjgl.opengl.Display;

/** Bounded optional presentation work; no chunk renderer/culler and no Argentum config writes. */
public final class OrnithePerformance {
  private final OrnitheClient c;
  public final OrnitheGameOptions options;
  public final DynamicFps dynamic = new DynamicFps();
  private int limit;
  private float appliedVolume = -1;

  public OrnithePerformance(OrnitheClient c) {
    this.c = c;
    options = new OrnitheGameOptions(c);
    c.capabilities.connect(
        c.modules.dynamicFps,
        State.EXPERIMENTAL,
        "Shared focus/minimized/AFK limits through the vanilla frame limiter. Sound attenuation is"
            + " transient; options are preserved.",
        c.modules.dynamicFps.settings().stream().map(s -> s.key()).toList(),
        "OrnithePerformance.frame",
        "MinecraftMixin.getMaxFramerate",
        "SoundManager.setVolume");
    c.capabilities.connect(
        c.modules.particles,
        State.EXPERIMENTAL,
        "Shared particle gate before native particle allocation, bounded live counts; no"
            + " replacement renderer",
        c.modules.particles.settings().stream().map(s -> s.key()).toList(),
        "ParticleManagerMixin.addParticle",
        "ParticleGate");
    c.capabilities.connect(
        c.modules.worldDetails,
        State.EXPERIMENTAL,
        "Native sky, star, weather and atlas animation gates. Fog remains Vanilla; no chunk"
            + " renderer changes.",
        List.of("sky", "stars", "weather", "animations"),
        "WorldDetailsMixin",
        "TextureAtlasMixin",
        "GameRendererMixin.renderSnowAndRain");
    c.capabilities.declare(
        "entityCulling",
        State.OPTIONAL_INTEGRATION,
        "No second entity culler is installed. External renderer/culling policy stays under modpack"
            + " control; saved preferences are retained.");
    c.capabilities.connect(
        c.modules.fpsBoost,
        State.LIMITED,
        "Explicit Vanilla-only detail presets with persistent shared UndoLog. No automatic preset,"
            + " no culler or Argentum config changes; no measured FPS gain is claimed.",
        List.of(),
        "OrnitheGameOptions",
        "BoostPreset.target",
        "UndoLog",
        "OrnitheMenuHost.actions");
  }

  public void frame() {
    long now = System.currentTimeMillis();
    boolean on = c.capabilities.active(c.modules.dynamicFps);
    if (on) {
      boolean any = c.mc.screen != null || Mouse.isButtonDown(0) || Mouse.isButtonDown(1);
      for (int i = 1; !any && i < Keyboard.KEYBOARD_SIZE; i++) any = Keyboard.isKeyDown(i);
      dynamic.input(now, Mouse.getX(), Mouse.getY(), any);
      boolean fullscreen = c.mc.isFullscreen(),
          minimized = DynamicFps.minimizedForLimit(fullscreen, !Display.isVisible());
      var state =
          dynamic.update(
              now,
              DynamicFps.focusedForLimit(fullscreen, minimized, Display.isActive()),
              minimized,
              c.modules.dynamicFpsAfkMinutes.get());
      limit =
          DynamicFps.limit(
              state,
              c.modules.dynamicFpsUnfocused.getInt(),
              c.modules.dynamicFpsMinimized.getInt(),
              c.modules.dynamicFpsAfk.getInt());
    } else {
      limit = 0;
      dynamic.touch(now);
    }
    float factor =
        on
            ? DynamicFps.volume(
                dynamic.state(),
                c.modules.dynamicFpsQuieter.get(),
                c.modules.dynamicFpsVolume.get())
            : 1;
    float volume =
        c.mc.options.getSoundCategoryVolume(net.minecraft.client.sound.SoundCategory.MASTER)
            * factor;
    if (volume != appliedVolume) {
      c.mc.getSoundManager().setVolume(net.minecraft.client.sound.SoundCategory.MASTER, volume);
      appliedVolume = volume;
    }
  }

  public int limit(int vanilla) {
    return limit > 0 ? Math.min(vanilla, limit) : vanilla;
  }
}
