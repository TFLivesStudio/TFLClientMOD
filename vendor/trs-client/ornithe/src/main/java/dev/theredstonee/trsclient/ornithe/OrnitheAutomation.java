package dev.theredstonee.trsclient.ornithe;

import dev.theredstonee.trsclient.core.chat.*;
import dev.theredstonee.trsclient.core.util.RateLimiter;
import dev.theredstonee.trsclient.ornithe.capability.CapabilityRegistry.State;
import net.minecraft.network.packet.s2c.play.ChatMessageS2CPacket;

/**
 * Newly ported senders require an explicit toggle in this platform; old preferences are preserved.
 */
public final class OrnitheAutomation {
  private final OrnitheClient c;
  private final AutoGg gg = new AutoGg();
  private final RateLimiter keys = new RateLimiter(1000, 3, 10000);
  private final boolean[] down = new boolean[4];
  private Object world;

  public OrnitheAutomation(OrnitheClient c) {
    this.c = c;
    for (var m : java.util.List.of(c.modules.autoGg, c.modules.textHotkeys))
      c.capabilities.connect(
          m,
          State.LIMITED,
          "Sends chat using shared sanitization/rate limits. Old enabled preferences do not"
              + " authorize sends: switch this module off/on here to approve TFL Client. Disable at"
              + " any time.",
          m.settings().stream().map(s -> s.key()).toList(),
          "OrnitheAutomation.tick",
          "ChatNetworkMixin.handleChatMessage",
          "OrnitheConsent",
          "AutoGg",
          "ChatOut");
    c.capabilities.connect(
        c.modules.qol.autoReconnect,
        State.EXPERIMENTAL,
        "Native disconnect countdown and cancel control, shared kick/ban guards and bounded"
            + " attempts. Old enabled flags require an explicit off/on toggle here.",
        c.modules.qol.autoReconnect.settings().stream().map(s -> s.key()).toList(),
        "ReconnectMixin",
        "AutoReconnect",
        "KickReasons",
        "OrnitheConsent");
    var settings =
        new java.util.ArrayList<>(
            c.modules.qol.queueAlerts.settings().stream().map(s -> s.key()).toList());
    settings.remove("flash");
    c.capabilities.connect(
        c.modules.qol.queueAlerts,
        State.LIMITED,
        "Shared queue/mention/death notices and native sounds from received chat/action bar. OS"
            + " taskbar flash is not connected.",
        settings,
        "ChatNetworkMixin.handleChatMessage",
        "Qol.onServerText",
        "QolPanels.drawWarnings");
  }

  public boolean allowed(dev.theredstonee.trsclient.core.module.Module module) {
    return c.capabilities.active(module)
        && c.consent.approved(module.id())
        && c.mc.player != null
        && c.mc.world != null
        && c.mc.player.getHealth() > 0;
  }

  public void receive(ChatMessageS2CPacket packet) {
    long now = System.currentTimeMillis();
    String raw = packet.getMessage().getFormattedString();
    dev.theredstonee.trsclient.core.cinematic.Cinematic.serverText(raw);
    c.map.onServerText(raw);
    if (packet.getType() == 2) c.pvp.qol.onServerText(raw, now);
    else if (allowed(c.modules.autoGg))
      gg.onMessage(
          packet.getMessage().getString(),
          AutoGg.extraTriggers(c.modules.autoGgTriggers.get()),
          c.modules.qol.autoGgPresets.get(),
          now,
          (long) (c.modules.autoGgDelay.get() * 1000));
  }

  public void tick() {
    if (c.mc.world != null && c.mc.getCurrentServerEntry() != null)
      c.pvp.qol.reconnect.inWorld(c.mc.getCurrentServerEntry().ip, System.currentTimeMillis());
    if (world != c.mc.world) {
      world = c.mc.world;
      if (world == null) c.pvp.qol.reconnect.leftWorld();
      gg.cancel();
      keys.reset();
      java.util.Arrays.fill(down, false);
    }
    long now = System.currentTimeMillis();
    if (!allowed(c.modules.autoGg) || c.mc.screen != null) gg.cancel();
    else if (gg.due(now)) send(c.modules.autoGgText.get());
    for (int i = 0; i < 4; i++) {
      boolean held = OrnitheKeys.isDown(c.modules.hotkeyKeys[i].get());
      if (held
          && !down[i]
          && c.mc.screen == null
          && allowed(c.modules.textHotkeys)
          && keys.tryAcquire(now)) send(c.modules.hotkeyTexts[i].get());
      down[i] = held;
    }
  }

  private void send(String text) {
    String clean = ChatOut.sanitize(text);
    if (!clean.isEmpty()) c.mc.player.sendChat(clean);
  }
}
