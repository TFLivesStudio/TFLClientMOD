package dev.theredstonee.trsclient.ornithe;

import dev.theredstonee.trsclient.core.config.ConfigStore;
import dev.theredstonee.trsclient.core.hud.*;
import dev.theredstonee.trsclient.core.i18n.I18n;
import dev.theredstonee.trsclient.core.input.*;
import dev.theredstonee.trsclient.core.module.HudModule;
import dev.theredstonee.trsclient.core.module.TrsModules;
import dev.theredstonee.trsclient.core.product.Product;
import dev.theredstonee.trsclient.core.ui.*;
import dev.theredstonee.trsclient.core.ui.menu.*;
import dev.theredstonee.trsclient.core.ui.title.*;
import dev.theredstonee.trsclient.core.zoom.ZoomState;
import java.util.*;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.menu.TitleScreen;
import net.minecraft.client.gui.screen.menu.multiplayer.MultiplayerScreen;
import net.minecraft.client.gui.screen.menu.options.OptionsScreen;
import net.minecraft.client.gui.screen.menu.world.SelectWorldScreen;
import net.minecraft.client.options.*;
import net.minecraft.client.render.Window;
import net.ornithemc.osl.entrypoints.api.ModInitializer;
import net.ornithemc.osl.keybinds.api.*;
import net.ornithemc.osl.lifecycle.api.client.MinecraftClientEvents;
import org.slf4j.*;

/** Ornithe platform hooks only. The registry, settings, layout, UI and storage belong to common. */
public final class OrnitheClient implements ModInitializer {
  public static final Logger LOG = LoggerFactory.getLogger("TFL/Ornithe");
  public static final Set<String> SUPPORTED =
      Set.of("fps", "coords", "cps", "keystrokes", "zoom", "toggleSprint", "crosshair");
  private static OrnitheClient instance;

  public static OrnitheClient get() {
    return instance;
  }

  public Minecraft mc;
  public OrnitheCanvas canvas;
  public final TrsModules modules = new TrsModules();
  public final ZoomState zoom = new ZoomState();
  public final ToggleState sprint = new ToggleState();
  public final ClickCounter left = new ClickCounter(), right = new ClickCounter();
  public final List<HudItem> hudItems = new ArrayList<>();
  public KeyBinding menuKey, zoomKey;
  public boolean ready, crosshairVisible;
  public ConfigStore config;
  private boolean sprintDown;
  private Object lastWorld;
  private Window window;
  private int windowWidth = -1, windowHeight = -1, guiScale = -1;
  private List<int[]> reticle = List.of(), outline = List.of();
  private String fps = "FPS", coords = "XYZ", cps = "CPS", sprintLabel = "Sprint OFF";
  public final long[] frameNanos = new long[8192];
  public int frameCount;
  private long lastFrame;
  private Runnable smokeTick;

  public String version() {
    return FabricLoader.getInstance()
        .getModContainer("tflcore")
        .orElseThrow()
        .getMetadata()
        .getVersion()
        .getFriendlyString();
  }

  @Override
  public void init() {
    instance = this;
    var dir = FabricLoader.getInstance().getConfigDir();
    I18n.init(dir);
    Theme.loadFrom(dir);
    config = new ConfigStore(dir.resolve(Product.CONFIG));
    LOG.info("Configuration: {}", config.load(modules.registry));
    KeybindEvents.REGISTER_KEYBINDS.register(
        () -> {
          menuKey = KeybindRegistry.register("key.tflcore.menu", 54, "key.categories.tflcore");
          zoomKey = KeybindRegistry.register("key.tflcore.zoom", 47, "key.categories.tflcore");
        });
    MinecraftClientEvents.READY.register(
        game -> {
          mc = game;
          canvas = new OrnitheCanvas(mc);
          reload();
          createHud();
          ready = true;
          if (mc.screen instanceof TitleScreen && !Product.safeMode()) mc.openScreen(title());
          LOG.info(
              "TFL Client {} ready: Minecraft 1.8.9 / Ornithe Gen2 / Java {} / LWJGL3={} /"
                  + " Argentum={}",
              version(),
              System.getProperty("java.version"),
              FabricLoader.getInstance().isModLoaded("legacy-lwjgl3"),
              FabricLoader.getInstance().isModLoaded("argentum"));
          if (FabricLoader.getInstance().isDevelopmentEnvironment()
              && Boolean.getBoolean("tflcore.ornithe.smoke"))
            try {
              smokeTick =
                  (Runnable)
                      Class.forName("dev.theredstonee.trsclient.ornithe.dev.SmokeHarness")
                          .getConstructor(OrnitheClient.class)
                          .newInstance(this);
            } catch (ReflectiveOperationException e) {
              throw new IllegalStateException(e);
            }
        });
    MinecraftClientEvents.TICK_END.register(
        game -> {
          if (ready) tick();
        });
    MinecraftClientEvents.STOP.register(
        game -> {
          save();
          config.flush();
        });
  }

  public void reload() {
    if (canvas != null) canvas.prepare();
    rebuildCrosshair();
  }

  public void save() {
    rebuildCrosshair();
    try {
      config.saveLater(modules.registry);
    } catch (java.io.IOException e) {
      LOG.error("Could not save TFL preferences", e);
    }
  }

  public void rebuildCrosshair() {
    reticle =
        Crosshair.rects(
            modules.crosshairShape.get(),
            (int) modules.crosshairSize.get(),
            (int) modules.crosshairGap.get(),
            (int) modules.crosshairThickness.get());
    outline = Crosshair.outline(reticle);
  }

  public Window window() {
    if (window == null
        || mc.width != windowWidth
        || mc.height != windowHeight
        || mc.options.guiScale != guiScale) {
      windowWidth = mc.width;
      windowHeight = mc.height;
      guiScale = mc.options.guiScale;
      window = new Window(mc);
    }
    return window;
  }

  public void openMenu(Screen parent) {
    mc.openScreen(new CoreScreen(new PocMenu(new PocHost(this, parent)), false));
  }

  public Screen title() {
    return new CoreScreen(
        new TitleUi(
            new TitleHost() {
              private Screen parent() {
                return mc.screen;
              }

              public void singleplayer() {
                mc.openScreen(new SelectWorldScreen(parent()));
              }

              public void multiplayer() {
                mc.openScreen(new MultiplayerScreen(parent()));
              }

              public void options() {
                mc.openScreen(new OptionsScreen(parent(), mc.options));
              }

              public void trsMenu() {
                openMenu(parent());
              }

              public boolean hasMods() {
                return false;
              }

              public void mods() {}

              public void quit() {
                mc.stop();
              }

              public void classicTitle() {
                if (Product.safeMode()) mc.openScreen(new TitleScreen());
              }

              public String versionLine() {
                return "Minecraft 1.8.9 · Ornithe Gen2 · Experimental";
              }

              public String clientVersion() {
                return version();
              }

              public void playClick() {}

              public void narrate(String text) {}

              public boolean animated() {
                return false;
              }

              public boolean simpleAnimation() {
                return true;
              }
            }),
        true);
  }

  private void tick() {
    while (menuKey.consumeClick()) {
      if (mc.screen instanceof CoreScreen cs && !cs.home) mc.openScreen(null);
      else openMenu(mc.screen);
    }
    boolean physical = GameOptions.isPressed(mc.options.sprintKey);
    boolean blocked = mc.world == null || mc.player == null || mc.screen != null;
    if (blocked || lastWorld != mc.world) sprint.reset();
    sprint.update(physical && !sprintDown ? 1 : 0, modules.toggleSprint.isEnabled(), blocked);
    sprintDown = physical;
    lastWorld = mc.world;
    fps = Minecraft.getCurrentFps() + " FPS";
    if (mc.player != null)
      coords =
          "XYZ "
              + (int) Math.floor(mc.player.x)
              + " / "
              + (int) Math.floor(mc.player.y)
              + " / "
              + (int) Math.floor(mc.player.z);
    long now = System.nanoTime() / 1_000_000;
    cps = left.count(now) + " | " + right.count(now) + " CPS";
    sprintLabel = sprint.active() ? "Sprint ON" : "Sprint OFF";
    if (smokeTick != null) smokeTick.run();
  }

  public void frame() {
    if (!ready) return;
    long now = System.nanoTime();
    if (lastFrame != 0) frameNanos[frameCount++ % frameNanos.length] = now - lastFrame;
    lastFrame = now;
    boolean active =
        modules.zoom.isEnabled()
            && mc.world != null
            && mc.player != null
            && mc.screen == null
            && zoomKey.isPressed();
    zoom.frame(active, false, modules.zoomFactor.get(), modules.zoomSmooth.get(), now);
  }

  public boolean sprintHeld(KeyBinding key) {
    return ready
        && key == mc.options.sprintKey
        && modules.toggleSprint.isEnabled()
        && sprint.active()
        && mc.screen == null
        && mc.world != null;
  }

  public void click(int code) {
    if (ready && mc.world != null && mc.player != null && mc.screen == null) {
      long now = System.nanoTime() / 1_000_000;
      if (code == -100) left.record(now);
      if (code == -99) right.record(now);
    }
  }

  private void createHud() {
    hudItems.add(item(modules.fps, () -> fps));
    hudItems.add(item(modules.coords, () -> coords));
    hudItems.add(item(modules.cps, () -> cps));
    hudItems.add(item(modules.toggleSprint, () -> sprintLabel));
    hudItems.add(
        new HudItem() {
          public HudModule module() {
            return modules.keystrokes;
          }

          public int width() {
            return 74;
          }

          public int height() {
            return 63;
          }

          public void draw(Canvas c) {
            key(c, mc.options.forwardKey, "W", 25, 0, 23);
            key(c, mc.options.leftKey, "A", 0, 21, 23);
            key(c, mc.options.backKey, "S", 25, 21, 23);
            key(c, mc.options.rightKey, "D", 50, 21, 23);
            key(c, mc.options.jumpKey, "SPACE", 0, 42, 73);
          }

          private void key(Canvas c, KeyBinding k, String text, int x, int y, int w) {
            c.fill(
                x,
                y,
                x + w,
                y + 19,
                k.isPressed() ? 0xC06078B0 : modules.keystrokes.backgroundArgb());
            c.text(
                text,
                x + (w - c.textWidth(text)) / 2,
                y + 5,
                modules.keystrokes.textColor.argb(),
                modules.keystrokes.shadow());
          }
        });
  }

  private HudItem item(HudModule module, java.util.function.Supplier<String> text) {
    return new HudItem() {
      public HudModule module() {
        return module;
      }

      public int width() {
        return canvas.textWidth(text.get()) + 8;
      }

      public int height() {
        return 18;
      }

      public void draw(Canvas c) {
        c.fill(0, 0, width(), height(), module.backgroundArgb());
        c.text(text.get(), 4, 4, module.textColor.argb(), module.shadow());
      }
    };
  }

  public void drawHud() {
    if (!ready || mc.world == null || mc.player == null || mc.options.hideGui) return;
    if (mc.screen instanceof CoreScreen cs && cs.ui instanceof HudEditor) return;
    Window win = window();
    canvas.begin(win.getWidth(), win.getHeight(), win.getScale());
    for (HudItem item : hudItems) {
      HudModule m = item.module();
      if (!m.isEnabled()) continue;
      float s = (float) m.scale.get();
      int x = HudLayout.resolveX(m.position(), Math.round(item.width() * s), win.getWidth()),
          y = HudLayout.resolveY(m.position(), Math.round(item.height() * s), win.getHeight());
      canvas.push();
      canvas.translate(x, y);
      canvas.scale(s);
      item.draw(canvas);
      canvas.pop();
    }
    if (crosshairVisible && modules.crosshair.isEnabled()) {
      int x = win.getWidth() / 2, y = win.getHeight() / 2;
      if (modules.crosshairOutline.get())
        for (int[] r : outline) canvas.fill(x + r[0], y + r[1], x + r[2], y + r[3], 0xFF000000);
      for (int[] r : reticle)
        canvas.fill(x + r[0], y + r[1], x + r[2], y + r[3], modules.crosshairColor.argb());
    }
  }
}
