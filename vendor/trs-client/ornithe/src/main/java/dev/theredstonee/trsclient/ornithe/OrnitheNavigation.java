package dev.theredstonee.trsclient.ornithe;

import dev.theredstonee.trsclient.core.screenshot.Screenshots;
import dev.theredstonee.trsclient.core.ui.*;
import dev.theredstonee.trsclient.core.ui.clips.*;
import dev.theredstonee.trsclient.core.ui.menu.*;
import dev.theredstonee.trsclient.core.ui.menus.*;
import dev.theredstonee.trsclient.ornithe.capability.CapabilityRegistry.State;
import java.util.*;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.menu.options.OptionsScreen;

/** Native navigation only. Module/editor/gallery/profile behavior remains in common. */
public final class OrnitheNavigation {
  private final OrnitheClient c;
  private Screen screenshotParent;
  public final Screenshots screenshots;

  public OrnitheNavigation(OrnitheClient c) {
    this.c = c;
    screenshots =
        Screenshots.install(
            new Screenshots.Platform() {
              public java.nio.file.Path gameDir() {
                return c.mc.gameDir.toPath();
              }

              public java.nio.file.Path configDir() {
                return FabricLoader.getInstance().getConfigDir();
              }

              public boolean openUi(String title, UiScreen ui) {
                screenshotParent = c.mc.screen;
                c.mc.openScreen(new CoreScreen(ui, false));
                return true;
              }

              public void closeUi() {
                c.mc.openScreen(screenshotParent);
              }

              public boolean copyText(String text) {
                try {
                  Screen.setClipboard(text);
                  return true;
                } catch (RuntimeException e) {
                  return false;
                }
              }

              public void notice(String text) {
                c.mapPlatform.message(text);
              }

              public void playClick() {
                c.clickSound();
              }

              public boolean online() {
                return false;
              }

              public boolean allowExternalPreferenceChanges() {
                return false;
              }
            },
            c.modules);
    c.capabilities.connect(
        c.modules.comfort.keySearch,
        State.LIMITED,
        "Shared search language, conflicts/unbound filters and native keyboard/mouse rebinding."
            + " Available through the module action; Vanilla Controls remains available.",
        List.of(),
        "OrnitheKeySearchUi",
        "KeySearch.filter",
        "KeyBinding.resetMapping",
        "GameOptions.save");
    c.capabilities.connect(
        c.modules.menuStyle,
        State.LIMITED,
        "Shared pause navigation and modern native option/button surfaces; vanilla behavior"
            + " retained",
        List.of(),
        "MinecraftMixin.openScreen",
        "ButtonWidgetMixin.render",
        "ScreenMixin.renderBackground");
    c.capabilities.connect(
        c.modules.comfort.screenshots,
        State.LIMITED,
        "Local gallery/editor and screenshot toast; online sharing, chat actions and third-party"
            + " preference changes are disabled",
        List.of("toast", "seconds"),
        "Screenshots.Platform",
        "Screenshots.tick",
        "Screenshots.render",
        "ClipsUi");
    c.capabilities.connect(
        c.modules.comfort.serverProfiles,
        State.LIMITED,
        "Existing local server-profile matching and shared profile UI",
        List.of("notify"),
        "ServerProfiles.update",
        "ModMenu.ServerProfilesPage");
  }

  public Screen pause() {
    var actions = new ArrayList<MenuAction>();
    var extras = new ArrayList<MenuAction>();
    actions.add(new MenuAction(dev.theredstonee.trsclient.core.i18n.I18n.tr("pause.resume"), "play", () -> c.mc.openScreen(null)));
    actions.add(new MenuAction("TFL Client", "settings", () -> c.openMenu(c.mc.screen)));
    actions.add(
        new MenuAction(
            dev.theredstonee.trsclient.core.i18n.I18n.tr("pause.options"),
            "settings",
            () -> c.mc.openScreen(new OptionsScreen(c.mc.screen, c.mc.options))));
    actions.add(new MenuAction(dev.theredstonee.trsclient.core.i18n.I18n.tr("pause.captures"), "image", () -> captures(c.mc.screen)));
    actions.add(new MenuAction(dev.theredstonee.trsclient.core.i18n.I18n.tr(c.mc.isSingleplayer()?"pause.singleplayerWorldInformation":"pause.serverInformation"), "info", () -> serverInfo(c.mc.screen)));
    extras.add(
        new MenuAction(dev.theredstonee.trsclient.core.i18n.I18n.tr("pause.diagnostics"), "activity", () -> diagnostics(c.mc.screen)));
    actions.add(
        new MenuAction(
            c.mc.isSingleplayer()?dev.theredstonee.trsclient.core.i18n.I18n.tr("pause.saveQuit"):dev.theredstonee.trsclient.core.i18n.I18n.tr("pause.disconnect"),
            "back",
            () -> {
              c.mc.world.disconnect();
              c.mc.setWorld(null);
              c.mc.openScreen(c.title());
            }));
    extras.add(new MenuAction(net.minecraft.client.resource.language.I18n.translate("gui.achievements"), "flag", () -> c.mc.openScreen(new net.minecraft.client.gui.screen.menu.AchievementsScreen(c.mc.screen,c.mc.player.getStats()))));
    extras.add(new MenuAction(net.minecraft.client.resource.language.I18n.translate("gui.stats"), "gauge", () -> c.mc.openScreen(new net.minecraft.client.gui.screen.menu.StatsScreen(c.mc.screen,c.mc.player.getStats()))));
    if(c.mc.isSingleplayer() && c.mc.getServer()!=null && !c.mc.getServer().isPublished())
      extras.add(new MenuAction(net.minecraft.client.resource.language.I18n.translate("menu.shareToLan"), "globe", () -> c.mc.openScreen(new net.minecraft.client.gui.screen.menu.world.OpenToLanScreen(c.mc.screen))));
    return new PauseChatScreen(new dev.theredstonee.trsclient.core.ui.menus.PauseActionsUi(
            dev.theredstonee.trsclient.core.i18n.I18n.tr("pause.title"), actions, extras, () -> c.mc.openScreen(null), c::clickSound));
  }

  public void captures(Screen parent) {
    c.mc.openScreen(
        new CoreScreen(
            new ClipsUi(
                new ClipsHost() {
                  public void playClick() {
                    c.clickSound();
                  }

                  public void closeScreen() {
                    c.mc.openScreen(parent);
                  }

                  public java.nio.file.Path gameDir() {
                    return c.mc.gameDir.toPath();
                  }

                  public java.nio.file.Path configDir() {
                    return FabricLoader.getInstance().getConfigDir();
                  }

                  public boolean copy(String text) {
                    try {
                      Screen.setClipboard(text);
                      return true;
                    } catch (RuntimeException e) {
                      return false;
                    }
                  }
                }),
            false));
  }

  public void diagnostics(Screen parent) {
    List<MenuAction> rows = new ArrayList<>();
    rows.add(new MenuAction("Export measured frame diagnostics","activity",()->OrnitheDiagnostics.export(c)));
    for (var m : c.modules.registry.all()) {
      var capability = c.capabilities.get(m.id());
      rows.add(
          new MenuAction(
              m.name() + " · " + capability.state(),
              m.icon(),
              () -> {
                if (c.capabilities.runnable(m))
                  c.mc.openScreen(
                      new CoreScreen(new ModMenu(new OrnitheMenuHost(c, parent)).select(m), false));
                else
                  c.mc.openScreen(
                      new CoreScreen(
                          new ActionListUi(
                              m.name(),
                              capability.detail(),
                              () -> diagnostics(parent),
                              c::clickSound),
                          false));
              }));
    }
    c.mc.openScreen(
        new CoreScreen(
            new ActionListUi(
                "TFL Client · " + c.modules.registry.all().size() + " capabilities",
                rows,
                () -> c.mc.openScreen(parent),
                c::clickSound),
            false));
  }

  public void serverInfo(Screen parent) {
    var info = new ServerInfoHost.Info();
    var s = c.mc.getCurrentServerEntry();
    info.singleplayer = c.mc.isSingleplayer();
    info.name = info.singleplayer&&c.mc.world!=null?c.mc.world.getData().getName():s == null ? "Local World" : s.name;
    info.address = s == null ? null : s.ip;
    info.version = "1.8.9";
    info.dimension = c.mapPlatform.dimension();
    if (c.mc.player != null)
      info.position =
          dev.theredstonee.trsclient.core.format.HudFormat.coords(
              c.mc.player.x, c.mc.player.y, c.mc.player.z);
    if (c.mc.getNetworkHandler() != null)
      info.players = c.mc.getNetworkHandler().getOnlinePlayers().size();
    c.mc.openScreen(
        new CoreScreen(
            new ServerInfoUi(
                new ServerInfoHost() {
                  public void playClick() {
                    c.clickSound();
                  }

                  public void closeScreen() {
                    c.mc.openScreen(parent);
                  }

                  public Info info() {
                    return info;
                  }

                  public boolean copy(String text) {
                    try {
                      Screen.setClipboard(text);
                      return true;
                    } catch (RuntimeException e) {
                      return false;
                    }
                  }

                  public String userAgent() {
                    return "TFLClient/" + c.version();
                  }
                },
                null),
            false));
  }
}
