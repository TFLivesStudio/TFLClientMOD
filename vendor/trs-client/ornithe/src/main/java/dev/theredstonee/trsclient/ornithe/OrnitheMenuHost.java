package dev.theredstonee.trsclient.ornithe;

import dev.theredstonee.trsclient.core.module.Module;
import dev.theredstonee.trsclient.core.module.TrsModules;
import dev.theredstonee.trsclient.core.ui.menu.*;
import java.util.*;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.options.GameOptions;

public final class OrnitheMenuHost implements MenuHost {
  private final OrnitheClient client;
  private final Screen parent;

  public OrnitheMenuHost(OrnitheClient client, Screen parent) {
    this.client = client;
    this.parent = parent;
  }

  public TrsModules modules() {
    return client.modules;
  }

  public void playClick() {
    client.clickSound();
  }

  public void closeScreen() {
    client.mc.openScreen(parent);
  }

  public void openHudEditor() {
    client.mc.openScreen(
        new CoreScreen(new HudEditor(new OrnitheMenuHost(client, client.mc.screen)), false));
  }

  public void openMenu() {
    if (parent instanceof CoreScreen) client.mc.openScreen(parent);
    else client.openMenu(parent);
  }

  public void openPacks() {
    client.mc.openScreen(
        new net.minecraft.client.gui.screen.menu.resourcepacks.ResourcePacksScreen(
            client.mc.screen));
  }

  public boolean hasClips() {
    return true;
  }

  public void openClips() {
    client.navigation.captures(client.mc.screen);
  }

  public String clipboard() {
    try {
      return Screen.getClipboard();
    } catch (RuntimeException e) {
      return null;
    }
  }

  public boolean hasPacks() {
    return true;
  }

  public boolean supports(Module m) {
    return client.capabilities.runnable(m);
  }

  public java.util.List<dev.theredstonee.trsclient.core.module.Setting> settings(Module m) {
    return client.capabilities.settings(m);
  }

  public String capabilityDetail(Module m) {
    return client.capabilities.get(m.id()).detail();
  }

  public List<MenuAction> actions(Module module) {
    var actions = new ArrayList<MenuAction>();
    if (module == client.modules.circuits.circuitLibrary)
      actions.add(
          new MenuAction("Offline Circuit Library", "layers", () -> client.circuits.openMenu()));
    if (module == client.modules.fpsBoost) {
      for (var preset : dev.theredstonee.trsclient.core.perf.BoostPreset.values())
        actions.add(
            new MenuAction(
                "Apply Vanilla preset · " + preset.label(),
                "chip",
                () -> client.performance.options.preset(preset)));
      if (!client.modules.perfUndo.isEmpty())
        actions.add(
            new MenuAction("Undo Vanilla preset", "back", () -> client.performance.options.undo()));
    }
    if (module == client.modules.comfort.keySearch)
      actions.add(
          new MenuAction(
              "Search & edit keybindings",
              "keyboard",
              () ->
                  client.mc.openScreen(
                      new CoreScreen(new OrnitheKeySearchUi(client, client.mc.screen), false))));
    if (module == client.modules.worldMap
        || module == client.modules.minimap
        || module == client.modules.waypoints) {
      if (inWorld())
        actions.add(
            new MenuAction(
                "World Map & Waypoints", "map", () -> client.openWorldMap(client.mc.screen)));
    }
    if (module instanceof dev.theredstonee.trsclient.core.module.HudModule)
      actions.add(new MenuAction("HUD Editor", "move", this::openHudEditor));
    return actions;
  }

  public void setEnabled(Module module, boolean enabled) {
    if (!client.config.isReadOnly()) client.consent.explicitToggle(module.id(), enabled);
    module.setEnabled(enabled);
  }

  public void save() {
    client.save();
  }

  public boolean shiftDown() {
    return Screen.isShiftDown();
  }

  public String keyLabel(String name) {
    return OrnitheKeys.display(name);
  }

  public String keyNameOf(int key) {
    return OrnitheKeys.name(key);
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
