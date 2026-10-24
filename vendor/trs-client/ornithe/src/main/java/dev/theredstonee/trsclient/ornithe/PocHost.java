package dev.theredstonee.trsclient.ornithe;

import dev.theredstonee.trsclient.core.module.Module;
import dev.theredstonee.trsclient.core.module.TrsModules;
import dev.theredstonee.trsclient.core.ui.menu.*;
import java.util.*;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.options.GameOptions;

public final class PocHost implements MenuHost {
  private final OrnitheClient client;
  private final Screen parent;

  public PocHost(OrnitheClient client, Screen parent) {
    this.client = client;
    this.parent = parent;
  }

  public TrsModules modules() {
    return client.modules;
  }

  public void playClick() {} // No sound-device dependency in the experimental adapter.

  public void closeScreen() {
    client.mc.openScreen(parent);
  }

  public void openHudEditor() {
    client.mc.openScreen(
        new CoreScreen(new HudEditor(new PocHost(client, client.mc.screen)), false));
  }

  public void openMenu() {
    if (parent instanceof CoreScreen) client.mc.openScreen(parent);
    else client.openMenu(parent);
  }

  public void openPacks() {}

  public boolean hasPacks() {
    return false;
  }

  public boolean supports(Module m) {
    return OrnitheClient.SUPPORTED.contains(m.id());
  }

  public List<MenuAction> actions(Module module) {
    return Collections.emptyList();
  }

  public void save() {
    client.save();
  }

  public boolean shiftDown() {
    return Screen.isShiftDown();
  }

  public String keyLabel(String name) {
    return name;
  }

  public String keyNameOf(int key) {
    return "key.keyboard." + GameOptions.getKeyName(key).toLowerCase(java.util.Locale.ROOT);
  }

  public String menuKeyLabel() {
    return GameOptions.getKeyName(client.menuKey.getKeyCode());
  }

  public String profileKeyLabel() {
    return "HUD editor";
  }

  public List<HudItem> hudItems() {
    return client.hudItems;
  }

  public boolean inWorld() {
    return client.mc.world != null;
  }
}
