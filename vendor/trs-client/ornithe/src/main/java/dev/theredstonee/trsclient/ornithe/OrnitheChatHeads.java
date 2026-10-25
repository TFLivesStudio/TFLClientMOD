package dev.theredstonee.trsclient.ornithe;

import dev.theredstonee.trsclient.core.chatheads.*;
import dev.theredstonee.trsclient.ornithe.capability.CapabilityRegistry.State;
import java.util.*;
import net.minecraft.text.*;

/**
 * MPL-attributed shared sender detection, preserving native wrapped components and click actions.
 */
public final class OrnitheChatHeads {
  private final OrnitheClient c;
  private final ChatHeadCache cache = new ChatHeadCache();
  private Object world;
  public long drawn;

  public OrnitheChatHeads(OrnitheClient c) {
    this.c = c;
    c.capabilities.connect(
        c.modules.qol.chatHeads,
        State.EXPERIMENTAL,
        "Shared sender detection, bounded line cache and native authorized skin/hat textures. Only"
            + " the first wrapped line gets a head; existing lines update on chat reset.",
        java.util.List.of("hat"),
        "ChatHeadsMixin",
        "ChatHeadDetect(MPL-2.0)",
        "ChatHeadCache",
        "TextRenderUtils.wrapText",
        "PlayerInfo.getSkinTexture");
  }

  public void tick() {
    if (world != c.mc.world) {
      world = c.mc.world;
      cache.clear();
    }
  }

  public void clear() {
    cache.clear();
  }

  public java.util.List<Text> wrap(
      Text message,
      int width,
      net.minecraft.client.render.TextRenderer font,
      boolean colors,
      boolean force) {
    if (!c.capabilities.active(c.modules.qol.chatHeads) || c.mc.getNetworkHandler() == null)
      return net.minecraft.client.render.TextRenderUtils.wrapText(
          message, width, font, colors, force);
    var names = new ArrayList<String>();
    for (var info : c.mc.getNetworkHandler().getOnlinePlayers())
      names.add(info.getProfile().getName());
    var hit = ChatHeadDetect.find(message.getFormattedString(), names);
    ChatHeadSender sender =
        hit == null
            ? ChatHeadSender.NONE
            : hit.self ? ChatHeadSender.self() : ChatHeadSender.name(hit.name);
    var lines =
        net.minecraft.client.render.TextRenderUtils.wrapText(
            message,
            sender.hasHead() ? ChatHeadLayout.wrapWidth(width, ChatHeadLayout.LEGACY_PAD) : width,
            font,
            colors,
            force);
    if (!sender.hasHead()) return lines;
    List<Text> indented = new ArrayList<>(lines.size());
    for (int i = 0; i < lines.size(); i++) {
      Text text = new LiteralText("  ").append(lines.get(i));
      cache.put(text, i == 0 ? sender : ChatHeadSender.NONE);
      indented.add(text);
    }
    return indented;
  }

  public void draw(Text line, float x, float y, int color) {
    if (!c.capabilities.active(c.modules.qol.chatHeads) || c.mc.getNetworkHandler() == null) return;
    var sender = cache.get(line);
    if (sender == null || !sender.hasHead()) return;
    var info =
        sender.self && c.mc.player != null
            ? c.mc.getNetworkHandler().getOnlinePlayer(c.mc.player.getUuid())
            : c.mc.getNetworkHandler().getOnlinePlayer(sender.name);
    if (info == null) return;
    var texture = c.canvas.game(info.getSkinTexture().toString(), 64, 64);
    int tint = ChatHeadLayout.tint(color);
    if (tint == 0) return;
    var win = c.window();
    c.canvas.begin(win.getWidth(), win.getHeight(), win.getScale());
    try {
      c.canvas.push();
      c.canvas.translate(x, y);
      c.canvas.image(texture, 8, 8, 8, 8, tint);
      if (c.modules.qol.chatHeadsHat.get()) c.canvas.image(texture, 40, 8, 8, 8, tint);
      c.canvas.pop();
      drawn++;
    } finally {
      c.canvas.end();
    }
  }
}
