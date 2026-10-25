package dev.theredstonee.trsclient.ornithe;

import dev.theredstonee.trsclient.core.camera.*;
import dev.theredstonee.trsclient.ornithe.capability.CapabilityRegistry.State;

/** Camera-only angles. The entity and its network orientation are never rewritten. */
public final class OrnitheFreelook {
  private final OrnitheClient c;
  public final FreelookState state = new FreelookState();
  private final ServerList servers = new ServerList();
  private Object world;
  private int previous = -1, selected = -1;

  public OrnitheFreelook(OrnitheClient c) {
    this.c = c;
    c.capabilities.connect(
        c.modules.freelook,
        State.EXPERIMENTAL,
        "Independent native camera angles, shared hold/toggle and server exclusion list. Restores"
            + " perspective on GUI, death and world changes.",
        c.modules.freelook.settings().stream().map(s -> s.key()).toList(),
        "GameRendererMixin.transformCamera",
        "GameRendererMixin.updateLocalPlayerCamera",
        "OrnitheFreelook.frame");
  }

  public void frame() {
    boolean valid =
        c.mc.player != null
            && c.mc.world != null
            && c.mc.player.getHealth() > 0
            && c.mc.screen == null;
    if (world != c.mc.world) {
      stop();
      world = c.mc.world;
    }
    if (!valid) {
      stop();
      return;
    }
    var server = c.mc.getCurrentServerEntry();
    boolean wanted =
        state.wanted(
            OrnitheKeys.isDown(c.modules.freelookKey.get()),
            c.modules.freelookToggle.get(),
            valid,
            c.capabilities.active(c.modules.freelook),
            servers.contains(server == null ? null : server.ip, c.modules.freelookServers.get()));
    if (state.consumeBlockedNotice())
      c.mapPlatform.message("Freelook is disabled for this server.");
    if (wanted && !state.active()) {
      c.emotes.attack();
      state.start(c.mc.player.yaw, c.mc.player.pitch);
      previous = c.mc.options.perspective;
      selected = c.modules.freelookPerspective.get().cameraMode();
      c.mc.options.perspective = selected;
    } else if (!wanted && state.active()) stop();
  }

  public void stop() {
    state.stop();
    if (previous >= 0 && c.mc.options.perspective == selected) c.mc.options.perspective = previous;
    previous = selected = -1;
  }
}
