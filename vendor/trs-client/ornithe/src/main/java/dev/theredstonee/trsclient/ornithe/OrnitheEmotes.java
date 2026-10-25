package dev.theredstonee.trsclient.ornithe;

import dev.theredstonee.trsclient.core.emote.*;
import dev.theredstonee.trsclient.core.ui.wheel.EmoteWheel;
import dev.theredstonee.trsclient.ornithe.capability.CapabilityRegistry.State;
import java.util.*;
import net.minecraft.client.gui.screen.Screen;

/**
 * Offline-only controller and native model/camera bindings; never instantiates an online service.
 */
public final class OrnitheEmotes {
  private final OrnitheClient c;
  public final EmoteController controller;
  private final List<EmotePlayback.Mover> movers = new ArrayList<>(1);
  private final EmoteController.Camera camera;
  public long appliedPoses;

  public OrnitheEmotes(OrnitheClient c) {
    this.c = c;
    controller = new EmoteController(c.modules, null, true);
    camera =
        new EmoteController.Camera() {
          public int get() {
            return c.mc.options.perspective;
          }

          public void set(int value) {
            c.mc.options.perspective = value;
          }
        };
    c.capabilities.connect(
        c.modules.emotes,
        State.EXPERIMENTAL,
        "Local radial wheel/model animation only; no remote synchronization",
        List.of("camera"),
        "EmoteWheel",
        "EmoteController(local=true)",
        "HumanoidModelMixin.setupAnimation",
        "OrnitheEmotes.tick");
  }

  public void tick() {
    movers.clear();
    if (c.mc.player != null)
      movers.add(
          new EmotePlayback.Mover(
              c.mc.player.getUuid(), c.mc.player.x, c.mc.player.z, c.mc.player.isSneaking(), true));
    controller.tick(System.currentTimeMillis(), movers, camera);
  }

  public void open(Screen parent) {
    if (c.mc.player == null || !c.capabilities.active(c.modules.emotes)) return;
    c.mc.openScreen(
        new CoreScreen(
            new EmoteWheel(
                controller,
                new EmoteWheel.Host() {
                  public Boolean keyHeld() {
                    return c.emoteKey.isPressed();
                  }

                  public String keyName() {
                    return OrnitheKeys.display(OrnitheKeys.name(c.emoteKey.getKeyCode()));
                  }

                  public void close() {
                    c.mc.openScreen(parent);
                  }

                  public void actionBar(String text) {
                    c.mapPlatform.message(text);
                  }

                  public void click() {
                    c.clickSound();
                  }
                }),
            false));
  }

  public void attack() {
    controller.onAttack(System.currentTimeMillis());
  }
}
