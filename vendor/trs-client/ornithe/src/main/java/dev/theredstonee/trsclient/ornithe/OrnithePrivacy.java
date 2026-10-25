package dev.theredstonee.trsclient.ornithe;

import dev.theredstonee.trsclient.core.streamer.StreamerMode;
import dev.theredstonee.trsclient.ornithe.capability.CapabilityRegistry.State;
import java.util.*;

/**
 * Bounded presentation cache; original names/addresses are never changed in network/config data.
 */
public final class OrnithePrivacy {
  private final OrnitheClient c;
  private final Map<String, String> cache =
      new LinkedHashMap<>(128, .75f, true) {
        protected boolean removeEldestEntry(Map.Entry<String, String> e) {
          return size() > 512;
        }
      };
  private boolean down;
  private String settings = "";
  private Object world;
  private int ticks;

  public OrnithePrivacy(OrnitheClient c) {
    this.c = c;
    c.capabilities.connect(
        c.modules.qol.streamer,
        State.LIMITED,
        "Masks rendered names and address tokens in common/native text and clipboard. Original"
            + " server/config/export data remains intact.",
        c.modules.qol.streamer.settings().stream().map(s -> s.key()).toList(),
        "PrivacyTextMixin",
        "PrivacyClipboardMixin",
        "OrnitheCanvas",
        "StreamerMode",
        "OrnithePrivacy.tick");
  }

  public void tick() {
    var m = c.modules.qol;
    boolean key = OrnitheKeys.isDown(m.streamerKey.get());
    if (key && !down && c.mc.screen == null) {
      m.streamer.toggle();
      c.save();
    }
    down = key;
    String next =
        m.streamer.isEnabled()
            + "|"
            + m.streamerName.get()
            + "|"
            + m.streamerOthers.get()
            + "|"
            + m.streamerHideIp.get();
    if (!settings.equals(next) || world != c.mc.world) {
      settings = next;
      world = c.mc.world;
      cache.clear();
      c.pvp.qol.streamer.forgetPlayers();
    }
    if (++ticks % 20 == 0 && c.mc.getNetworkHandler() != null) {
      for (var info : c.mc.getNetworkHandler().getOnlinePlayers())
        c.pvp.qol.streamer.knowPlayer(info.getProfile().getName());
      cache.clear();
    }
  }

  public String text(String raw) {
    if (raw == null || !c.capabilities.active(c.modules.qol.streamer)) return raw;
    String hit = cache.get(raw);
    if (hit != null) return hit;
    String result = c.pvp.qol.streamer.replace(raw);
    if (c.modules.qol.streamerHideIp.get()) {
      var server = c.mc.getCurrentServerEntry();
      if (server != null && !server.ip.isEmpty())
        result = result.replace(server.ip, StreamerMode.HIDDEN_ADDRESS);
      StringBuilder masked = new StringBuilder(result.length());
      int i = 0;
      while (i < result.length()) {
        int end = i;
        while (end < result.length() && !Character.isWhitespace(result.charAt(end))) end++;
        String word = result.substring(i, end);
        String plain = dev.theredstonee.trsclient.core.chat.ChatText.strip(word);
        boolean address =
            StreamerMode.looksLikeAddress(plain)
                && (plain.chars().anyMatch(Character::isLetter)
                    || plain.chars().filter(ch -> ch == '.').count() >= 3
                    || plain.indexOf(':') >= 0);
        masked.append(address ? StreamerMode.HIDDEN_ADDRESS : word);
        if (end < result.length()) masked.append(result.charAt(end++));
        i = end;
      }
      result = masked.toString();
    }
    cache.put(raw, result);
    return result;
  }
}
