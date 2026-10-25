package dev.theredstonee.trsclient.ornithe;

import dev.theredstonee.trsclient.core.keys.KeySearch;
import dev.theredstonee.trsclient.core.ui.*;
import dev.theredstonee.trsclient.core.ui.menus.MenuSkin;
import dev.theredstonee.trsclient.core.ui.menus.WindowUi;
import java.util.*;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.options.*;

/** Shared key query language and window controls, backed by the actual Vanilla binding array. */
public final class OrnitheKeySearchUi extends WindowUi {
  private final OrnitheClient client;
  private final Screen parent;
  private final TextInput query = new TextInput(100);
  private final List<KeySearch.Entry> entries = new ArrayList<>();
  private List<KeySearch.Entry> filtered = List.of();
  private String built = "";
  private KeyBinding capture;
  private int scroll, maxScroll;

  public OrnitheKeySearchUi(OrnitheClient client, Screen parent) {
    this.client = client;
    this.parent = parent;
    query.setFocused(true);
    refresh();
  }

  protected String title() {
    return "Keybindings";
  }

  protected void playClick() {
    client.clickSound();
  }

  protected void onClosed() {
    client.mc.options.save();
    client.mc.openScreen(parent);
  }

  private void refresh() {
    entries.clear();
    for (var key : client.mc.options.keyBindings)
      entries.add(
          new KeySearch.Entry(
              net.minecraft.locale.I18n.translate(key.getName()),
              key.getName(),
              net.minecraft.locale.I18n.translate(key.getCategory()),
              key.getCategory(),
              OrnitheKeys.name(key.getKeyCode()),
              GameOptions.getKeyName(key.getKeyCode()),
              key));
    KeySearch.markConflicts(entries);
    filtered = KeySearch.filter(entries, query.text());
    built = query.text();
  }

  protected void content(Canvas canvas, int x, int y, int w, int h, int mx, int my, float dt) {
    if (!query.text().equals(built)) {
      scroll = 0;
      refresh();
    }
    var theme = Theme.get();
    MenuSkin.button(canvas, x, y, w, 22, query.focused(), true);
    canvas.text(
        canvas.clip(
            capture == null
                ? (query.isEmpty() ? "Search · key:r, mouse, conflict, unbound" : query.text())
                : "Press a key · Esc clears the binding",
            w - 14),
        x + 7,
        y + 6,
        theme.text,
        false);
    hits.add(x, y, w, 22, () -> query.setFocused(true));
    int top = y + 28, available = Math.max(0, h - 28);
    maxScroll = Math.max(0, filtered.size() * 27 - available);
    scroll = Math.min(scroll, maxScroll);
    canvas.scissor(x, top, x + w, y + h);
    hits.clip(x, top, w, available);
    for (int i = 0; i < filtered.size(); i++) {
      var entry = filtered.get(i);
      int row = top + i * 27 - scroll;
      if (row + 25 < top || row > y + h) continue;
      var key = (KeyBinding) entry.handle;
      canvas.text(canvas.clip(entry.name, w - 115), x + 4, row + 3, theme.text, false);
      canvas.text(canvas.clip(entry.category, w - 115), x + 4, row + 14, theme.textDim, false);
      button(
          canvas,
          x + w - 105,
          row,
          70,
          23,
          capture == key ? "…" : GameOptions.getKeyName(key.getKeyCode()),
          false,
          true,
          mx,
          my,
          () -> {
            capture = key;
            query.setFocused(false);
          });
      button(
          canvas,
          x + w - 31,
          row,
          30,
          23,
          "↺",
          false,
          true,
          mx,
          my,
          () -> assign(key, key.getDefaultKeyCode()));
    }
    hits.noClip();
    canvas.noScissor();
  }

  private void assign(KeyBinding binding, int code) {
    binding.setKeyCode(code);
    KeyBinding.resetMapping();
    client.mc.options.save();
    capture = null;
    refresh();
  }

  public boolean keyPressed(int raw, UiKey key, boolean shift) {
    if (capture != null) {
      assign(capture, key == UiKey.ESCAPE ? 0 : raw);
      return true;
    }
    if (key == UiKey.ESCAPE) return super.keyPressed(raw, key, shift);
    return query.key(key) || super.keyPressed(raw, key, shift);
  }

  public boolean charTyped(char ch) {
    return capture == null && query.type(ch);
  }

  public boolean mouseClicked(double x, double y, int button) {
    if (capture != null) {
      assign(capture, button - 100);
      return true;
    }
    query.setFocused(false);
    return super.mouseClicked(x, y, button);
  }

  public boolean mouseScrolled(double x, double y, double amount) {
    scroll = Math.max(0, Math.min(maxScroll, scroll - (int) (amount * 27)));
    return true;
  }

  public boolean wantsTextInput() {
    return capture == null && query.focused();
  }
}
