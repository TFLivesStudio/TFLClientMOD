package dev.theredstonee.trsclient.ornithe;

import dev.theredstonee.trsclient.core.module.HudModule;
import dev.theredstonee.trsclient.core.redstone.*;
import dev.theredstonee.trsclient.core.ui.*;
import dev.theredstonee.trsclient.core.ui.menu.HudItem;
import dev.theredstonee.trsclient.ornithe.capability.CapabilityRegistry.State;
import java.util.List;
import net.minecraft.world.HitResult;

/** Tick sampling and shared HUD/overlay renderers; no world scans from render. */
public final class OrnitheRedstone {
  private final OrnitheClient c;
  public final OrnitheRedstoneWorld world = new OrnitheRedstoneWorld();
  public final RedstoneTools tools;
  private final SignalOverlay overlay = new SignalOverlay();

  public OrnitheRedstone(OrnitheClient c) {
    this.c = c;
    tools = new RedstoneTools(c.modules);
    var signal = new RedstonePanels.Signal(c.modules, tools);
    var clock = new RedstonePanels.Clock(c.modules, tools);
    for (var m :
        List.of(c.modules.redstoneSignal, c.modules.redstoneClock, c.modules.redstoneOverlay))
      c.capabilities.connect(
          m,
          State.LIMITED,
          "Native 1.8.9 received block states and shared renderers. Container contents remain"
              + " unknown; client packet timing is not server TPS.",
          m.settings().stream().map(s -> s.key()).toList(),
          "OrnitheRedstone.tick",
          "OrnitheRedstoneWorld",
          "RedstoneTools",
          "RedstonePanels",
          "SignalOverlay");
    c.hudItems.add(
        new HudItem() {
          public HudModule module() {
            return c.modules.redstoneSignal;
          }

          public int width() {
            return signal.width(c.canvas::textWidth, true);
          }

          public int height() {
            return signal.visible() || preview() ? signal.height(preview()) : 1;
          }

          public void draw(Canvas canvas) {
            if (signal.visible() || preview()) signal.draw(canvas, c.canvas::textWidth, preview());
          }
        });
    c.hudItems.add(
        new HudItem() {
          public HudModule module() {
            return c.modules.redstoneClock;
          }

          public int width() {
            return clock.width(c.canvas::textWidth, true);
          }

          public int height() {
            return clock.visible() || preview() ? clock.height(preview()) : 1;
          }

          public void draw(Canvas canvas) {
            if (clock.visible() || preview()) clock.draw(canvas, c.canvas::textWidth, preview());
          }
        });
  }

  private boolean preview() {
    return c.mc.world == null
        || c.mc.screen instanceof CoreScreen cs
            && cs.ui instanceof dev.theredstonee.trsclient.core.ui.menu.HudEditor;
  }

  public void tick() {
    world.world = c.mc.world;
    if (c.mc.player == null) {
      tools.tick(null, null, false, 0, 0, 0, 0, 0, 0);
      return;
    }
    var hit = c.mc.crosshairTarget;
    boolean block = hit != null && hit.type == HitResult.Type.BLOCK;
    var p = block ? hit.getPos() : null;
    var eye = c.mc.player.getEyePosition(1);
    tools.tick(
        world,
        c.mc.world,
        block,
        block ? p.getX() : 0,
        block ? p.getY() : 0,
        block ? p.getZ() : 0,
        eye.x,
        eye.y,
        eye.z);
  }

  public void draw(Canvas canvas, int width, int height) {
    if (!c.capabilities.active(c.modules.redstoneOverlay)
        || c.mc.options.perspective != 0
        || c.mc.screen != null) return;
    var p = c.mc.player;
    overlay.draw(
        canvas,
        tools.cache().entries(),
        p.x,
        p.y + p.getEyeHeight(),
        p.z,
        p.yaw,
        p.pitch,
        c.worldFov,
        width,
        height,
        c.modules.redstoneOverlayZero.get());
  }
}
