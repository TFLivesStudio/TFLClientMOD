package dev.theredstonee.trsclient.ornithe;

import dev.theredstonee.trsclient.core.chat.*;
import dev.theredstonee.trsclient.ornithe.capability.CapabilityRegistry.State;
import java.time.LocalTime;
import java.util.*;
import net.minecraft.client.gui.chat.ChatGui;
import net.minecraft.text.*;

/** Local chat presentation. Never sends packets or instantiates remote services. */
public final class OrnitheChat {
  private final OrnitheClient c;
  private final ChatStacker stacker = new ChatStacker();
  private Object world;
  private int lastId, nextId = 0x74520000;

  public OrnitheChat(OrnitheClient c) {
    this.c = c;
    c.capabilities.connect(
        c.modules.chat,
        State.LIMITED,
        "Timestamps, stacking and bounded history preserve native text/click/hover components. Copy"
            + " and coordinate links are not connected yet.",
        List.of("timestamps", "timestampSeconds", "timestampTwelveHour", "history", "stack"),
        "ChatGuiMixin.addMessage",
        "ChatHistory.limit",
        "OrnitheChat.receive");
    c.capabilities.connect(
        c.modules.qol.chatFilter,
        State.LIMITED,
        "Shared bounded literal word matching before chat insertion; no outgoing interception",
        c.modules.qol.chatFilter.settings().stream().map(s -> s.key()).toList(),
        "Qol.hideChat",
        "ChatGuiMixin.addMessage");
    c.capabilities.connect(
        c.modules.qol.mentions,
        State.LIMITED,
        "Shared mention detection and throttled native sound; whole-line bold highlight. Exact RGB"
            + " word highlighting is not connected.",
        List.of("ownName", "words", "bold", "sound", "soundType", "volume"),
        "Qol.onChat",
        "ChatGuiMixin.addMessage");
  }

  public void tick() {
    if (world != c.mc.world) {
      world = c.mc.world;
      reset();
    }
  }

  public void reset() {
    stacker.reset();
    lastId = 0;
  }

  /** Returns true when the original insertion has been handled or intentionally filtered. */
  public boolean receive(ChatGui gui, Text message) {
    String raw = message.getFormattedString();
    long now = System.currentTimeMillis();
    // Read server fair-play markers before applying display filters.
    if (c.map != null) c.map.onServerText(raw);
    boolean mentioned = c.pvp.qol.onChat(raw, now);
    if (c.capabilities.active(c.modules.qol.chatFilter) && c.pvp.qol.hideChat(message.getString()))
      return true;
    boolean chat = c.capabilities.active(c.modules.chat),
        stack = chat && c.modules.chatStack.get(),
        timestamp = chat && c.modules.chatTimestamps.get();
    if (!stack) reset();
    if (!stack && !timestamp && !mentioned) return false;
    Text content = message.copy();
    if (mentioned && c.modules.qol.mentionsBold.get()) content.getStyle().setBold(true);
    Text result = new LiteralText("");
    if (timestamp) {
      LocalTime t = LocalTime.now();
      result.append(
          new LiteralText(
                  ChatTimestamp.format(
                      t.getHour(),
                      t.getMinute(),
                      t.getSecond(),
                      c.modules.chatTimestampSeconds.get(),
                      c.modules.qol.chatTwelveHour.get()))
              .setStyle(new Style().setColor(Formatting.DARK_GRAY)));
    }
    result.append(content);
    int id = 0;
    if (stack) {
      int count = stacker.accept(message.getString(), now, ChatStacker.DEFAULT_WINDOW_MS);
      if (count > 1 && lastId != 0) {
        gui.removeMessage(lastId);
        result.append(
            new LiteralText(ChatStacker.suffix(count))
                .setStyle(new Style().setColor(Formatting.GRAY)));
      }
      id = nextId++;
      lastId = id;
    }
    gui.addMessage(result, id);
    return true;
  }

  public int historyLimit() {
    return ChatHistory.limit(
        c.capabilities.active(c.modules.chat), c.modules.qol.chatHistory.get());
  }
}
