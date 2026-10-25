package dev.theredstonee.trsclient.ornithe;

import dev.theredstonee.trsclient.core.halloween.SpookyFx;
import dev.theredstonee.trsclient.core.ui.Canvas;
import dev.theredstonee.trsclient.ornithe.capability.CapabilityRegistry.State;

/** Procedural offline effects only; never starts the remote Halloween event/service. */
public final class OrnitheSeasonal {
  private final OrnitheClient c;
  private Object world;

  public OrnitheSeasonal(OrnitheClient c) {
    this.c = c;
    c.modules.halloween.halloween.availableWhen(() -> true);
    c.capabilities.connect(
        c.modules.halloween.halloween,
        State.LIMITED,
        "Local procedural bat hit effects and self ghost trail. Remote event rewards, other-client"
            + " effects, jumpscares and remote sounds are unavailable.",
        java.util.List.of("hits", "ghostTrail"),
        "OrnitheSeasonal.tick",
        "LivingEntityMixin.doEvent",
        "SpookyFx.draw");
  }

  public void tick() {
    var fx = SpookyFx.get();
    if (world != c.mc.world || !c.capabilities.active(c.modules.halloween.halloween)) {
      world = c.mc.world;
      fx.clear();
      return;
    }
    long now = System.currentTimeMillis();
    var p = c.mc.player;
    if (p != null && p.isSprinting() && c.modules.halloween.trail.get())
      fx.trail(p.getUuid(), p.x, p.y, p.z, p.yaw, now);
    fx.step(now);
  }

  public void hit(net.minecraft.entity.Entity target) {
    if (c.capabilities.active(c.modules.halloween.halloween) && c.modules.halloween.hits.get())
      SpookyFx.get().hit(target.x, target.y + target.getEyeHeight() / 2, target.z);
  }

  public void draw(Canvas canvas, int width, int height) {
    if (!c.capabilities.active(c.modules.halloween.halloween)
        || c.mc.player == null
        || c.mc.options.perspective != 0
        || c.cinematic.detached()) return;
    var p = c.mc.player;
    SpookyFx.get()
        .draw(canvas, p.x, p.y + p.getEyeHeight(), p.z, p.yaw, p.pitch, c.worldFov, width, height);
  }
}
