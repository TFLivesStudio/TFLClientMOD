package dev.theredstonee.trsclient.ornithe;
import net.minecraft.client.render.platform.GlStateManager;

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
  private static OrnitheClient instance;

  public static OrnitheClient get() {
    return instance;
  }

  public Minecraft mc;
  public OrnitheCanvas canvas;
  public final TrsModules modules = new TrsModules(true);
  public final dev.theredstonee.trsclient.ornithe.capability.CapabilityRegistry capabilities =
      new dev.theredstonee.trsclient.ornithe.capability.CapabilityRegistry(modules.registry);
  public final ZoomState zoom = new ZoomState();
  public final ToggleState sprint = new ToggleState(), sneak = new ToggleState();
  private boolean sneakDown;
  private int sneakPresses;
  private OrnitheHuds extendedHuds;
  public final ClickCounter left = new ClickCounter(), right = new ClickCounter();
  public final List<HudItem> hudItems = new ArrayList<>();
  public KeyBinding menuKey, zoomKey, mapKey, emoteKey;
  public OrnitheEmotes emotes;
  public OrnitheNavigation navigation;
  public OrnithePvp pvp;
  public OrnitheWaypoints waypointOverlay;
  public float worldFov = 70;
  public long initNanos=System.nanoTime(),readyNanos,worldLoadNanos;
  public double lastWorldLoadMs;
  public OrnithePanorama panorama;
  public OrnitheLatency latency;
  public OrnitheChatHeads heads;
  public OrnitheNet network;
  public OrnitheBorderless borderless;
  public OrnitheCircuits circuits;
  public OrnitheSeasonal seasonal;
  public OrnitheColors colors;
  public OrnitheCinematic cinematic;
  public OrnitheCapes capes;
  public OrnithePrivacy privacy;
  public OrnitheComfort comfort;
  public OrnitheRefill refill;
  public OrnitheAutomation automation;
  public OrnithePerformance performance;
  public OrnitheVanillaHuds vanillaHuds;
  public float hudPartial;
  public OrnitheFreelook freelook;
  public OrnitheChat chat;
  public OrnitheRedstone redstone;
  public dev.theredstonee.trsclient.core.notes.Notes notes;
  private String sharedZoomKey, sharedMapKey;
  private boolean notesDown;
  private int nativeZoomKey, nativeMapKey, lastSprintCode, lastSneakCode;
  public OrnitheMiniPlayer miniPlayer;
  public float hudPartialTick=1;
  public boolean ready, crosshairVisible;
  public ConfigStore config;
  public final OrnitheConsent consent = new OrnitheConsent();
  public dev.theredstonee.trsclient.core.waypoint.WaypointStore waypoints;
  public dev.theredstonee.trsclient.core.map.MapEngine map;
  public OrnitheMapPlatform mapPlatform;
  public OrnitheDiorama diorama;
  private final DioramaMinimapTarget minimapTarget=new DioramaMinimapTarget();
  private final dev.theredstonee.trsclient.core.map.MinimapRenderer minimapRenderer =
      new dev.theredstonee.trsclient.core.map.MinimapRenderer();

  private boolean sprintDown;
  private int sprintPresses;
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
    installCapabilities();
    installCamera();
    var dir = FabricLoader.getInstance().getConfigDir();
    I18n.init(dir);
    Theme.loadFrom(dir);
    waypoints =
        new dev.theredstonee.trsclient.core.waypoint.WaypointStore(dir.resolve(Product.WAYPOINTS));
    waypoints.load();
    modules.registry.addPart(consent);
    config = new ConfigStore(dir.resolve(Product.CONFIG));
    LOG.info("Configuration: {}", config.load(modules.registry));
    KeybindEvents.REGISTER_KEYBINDS.register(
        () -> {
          menuKey = KeybindRegistry.register("key.tflcore.menu", 54, "key.categories.tflcore");
          emoteKey = KeybindRegistry.register("key.tflcore.emotes", 19, "key.categories.tflcore");
          mapKey = KeybindRegistry.register("key.tflcore.map", 50, "key.categories.tflcore");
          zoomKey = KeybindRegistry.register("key.tflcore.zoom", 47, "key.categories.tflcore");
        });
    MinecraftClientEvents.READY.register(
        game -> {
          mc = game;
          canvas = new OrnitheCanvas(mc);
          map = dev.theredstonee.trsclient.core.map.MapEngine.init(modules, dir);
          mapPlatform = new OrnitheMapPlatform(this);
          installMaps();
          notes = dev.theredstonee.trsclient.core.notes.Notes.init(modules, dir, LOG::warn);
          emotes = new OrnitheEmotes(this);
          navigation = new OrnitheNavigation(this);
          pvp = new OrnithePvp(this);
          latency = new OrnitheLatency(this);
          heads = new OrnitheChatHeads(this);
          network = new OrnitheNet(this);
          borderless = new OrnitheBorderless(this);
          seasonal = new OrnitheSeasonal(this);
          colors = new OrnitheColors(this);
          capes = new OrnitheCapes(this);
          privacy = new OrnithePrivacy(this);
          automation = new OrnitheAutomation(this);
          refill = new OrnitheRefill(this);
          comfort = new OrnitheComfort(this);
          performance = new OrnithePerformance(this);
          vanillaHuds = new OrnitheVanillaHuds(this);
          freelook = new OrnitheFreelook(this);
          chat = new OrnitheChat(this);
          redstone = new OrnitheRedstone(this);
          circuits = new OrnitheCircuits(this);
          cinematic = new OrnitheCinematic(this);
          waypointOverlay = new OrnitheWaypoints(this);
          panorama = new OrnithePanorama(this);
          capabilities.connect(
              modules.notes.notes,
              dev.theredstonee.trsclient.ornithe.capability.CapabilityRegistry.State.LIMITED,
              "Shared offline notebooks and map coordinate actions; remote sync disabled",
              List.of("key"),
              "Notes.tick",
              "ModMenu.NotesPage",
              "MapPlatform");
          dev.theredstonee.trsclient.core.ui.background.MenuBackground.shared().install(new dev.theredstonee.trsclient.core.ui.background.LocalMjpegPlayer(dir.resolve("tflcore/menu.mjpeg")));
          reload();
          createHud();
          extendedHuds = new OrnitheHuds(this);
          var notePanel = new dev.theredstonee.trsclient.core.notes.NotePanel(modules);
          capabilities.connect(
              modules.notes.pinnedNote,
              dev.theredstonee.trsclient.ornithe.capability.CapabilityRegistry.State.LIMITED,
              "Shared pinned notebook, checklist and wrapping; offline only",
              modules.notes.pinnedNote.settings().stream().map(s -> s.key()).toList(),
              "NotePanel",
              "Notes.currentWorld",
              "GameGuiMixin.render");
          hudItems.add(
              new HudItem() {
                public HudModule module() {
                  return modules.notes.pinnedNote;
                }

                private boolean preview() {
                  return mc.world == null
                      || mc.screen instanceof CoreScreen cs && cs.ui instanceof HudEditor;
                }

                public int width() {
                  return notePanel.width(canvas::textWidth, preview());
                }

                public int height() {
                  return notePanel.height(canvas::textWidth, preview());
                }

                public void draw(Canvas c) {
                  if (preview() || notePanel.visible())
                    notePanel.draw(c, canvas::textWidth, preview());
                }
              });
          synchronizeKeys();
          readyNanos=System.nanoTime();
          ready = true;
          if (mc.screen instanceof TitleScreen && !Product.safeMode()) mc.openScreen(title());
          LOG.info(
              "TFL Client {} ready: Minecraft 1.8.9 / Ornithe Gen2 / Java {} / LWJGL3={} /"
                  + " Argentum={}",
              version(),
              System.getProperty("java.version"),
              FabricLoader.getInstance().isModLoaded("legacy-lwjgl3"),
              FabricLoader.getInstance().isModLoaded("argentum"));
          if ((FabricLoader.getInstance().isDevelopmentEnvironment()||FabricLoader.getInstance().isModLoaded("tfl_depth_probe"))
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
          if (emotes != null) emotes.controller.tick(System.currentTimeMillis(), List.of(), null);
          if (notes != null) notes.save();
          if (latency != null) latency.close();
          if (colors != null) colors.release();
          if (cinematic != null) cinematic.detachView(false);
          minimapTarget.close();
          if (diorama != null)diorama.close();
          if (map != null) map.shutdown();
          try {
            waypoints.saveIfDirty();
          } catch (java.io.IOException e) {
            LOG.error("Waypoint shutdown save failed", e);
          }
        });
  }

  private void installCapabilities() {
    var limited = dev.theredstonee.trsclient.ornithe.capability.CapabilityRegistry.State.LIMITED;
    for (var item :
        List.of(modules.fps, modules.cps, modules.coords, modules.keystrokes, modules.toggleSprint))
      capabilities.connect(
          item,
          limited,
          "TFL HUD; advanced options are listed only when connected",
          List.of("scale", "shadow", "background", "backgroundOpacity", "textColor"),
          "GameGuiMixin.render",
          "OrnitheClient.createHud",
          "OrnitheClient.tick");
    for (var item : List.of(modules.coords, modules.keystrokes))
      capabilities.connect(
          item,
          limited,
          "Native coordinates/direction/biome and actual bindings with mouse/CPS/space options",
          item.settings().stream().map(s -> s.key()).toList(),
          "OrnitheHuds.tick",
          "OrnitheKeystrokes",
          "GameGuiMixin.render");
    capabilities.connect(
        modules.zoom,
        limited,
        "Factor and smoothing connected; extended camera options in migration",
        List.of("factor", "smooth"),
        "GameRendererMixin.getFov",
        "OrnitheClient.frame");
    capabilities.connect(
        modules.crosshair,
        limited,
        "Basic geometry connected; 1.8.9 has no attack cooldown indicator",
        List.of("shape", "color", "size", "gap", "thickness", "outline"),
        "GameGuiMixin.hasCrosshair",
        "OrnitheClient.drawHud");
    capabilities.connect(
        modules.titleScreen,
        limited,
        "Shared TFL title and asynchronous optional local video at config/tflcore/menu.mjpeg; static black-hole fallback when unavailable",
        List.of("animated","backgroundMode","menuFpsLimit","panelOpacity","cornerRadius","menuAccent"),
        "MinecraftMixin.openScreen",
        "OrnitheClient.title");
    var backend =
        dev.theredstonee.trsclient.ornithe.capability.CapabilityRegistry.State.BACKEND_UNAVAILABLE;
    for (String id : List.of("trsOnline", "social", "clips"))
      capabilities.declare(
          id,
          backend,
          "Requires an authorized TFL backend/launcher integration; saved preferences retained");
    capabilities.declare(
        "shieldPosition",
        dev.theredstonee.trsclient.ornithe.capability.CapabilityRegistry.State.VERSION_UNAVAILABLE,
        "Combat shields were introduced in Minecraft 1.9");
    capabilities.declare(
        "builtinOptimizations",
        dev.theredstonee.trsclient.ornithe.capability.CapabilityRegistry.State.OPTIONAL_INTEGRATION,
        "External renderer/optimizers remain separate; no optimization binary is bundled");
  }

  public void reload() {
    if(miniPlayer!=null)miniPlayer.close();
    if (colors != null) colors.release();
    if (canvas != null) canvas.prepare();
    rebuildCrosshair();
    if (mapPlatform != null) mapPlatform.reloaded();
    minimapTarget.close();
    if(diorama!=null)diorama.reload();
    if (map != null) {
      map.textures().clear();
      map.sprites().clear();
    }
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

  private void installMaps() {
    var limited = dev.theredstonee.trsclient.ornithe.capability.CapabilityRegistry.State.LIMITED;
    for (var m : List.of(modules.minimap, modules.worldMap))
      capabilities.connect(
          m,
          limited,
          "Diorama voxel map over received chunks; connected legacy block bounds are approximated",
          m.settings().stream().map(setting -> setting.key()).toList(),
          "OrnitheDiorama.tick",
          "OrnitheMapPlatform",
          "DioramaMinimapTarget",
          "OrnitheDioramaUi");
    capabilities.connect(
        modules.waypoints,
        limited,
        "Persistent points and map controls; world-space beams are not connected yet",
        List.of("death"),
        "WaypointStore",
        "WorldMapUi",
        "OrnitheMapPlatform");
    hudItems.add(
        new HudItem() {
          public HudModule module() {
            return modules.minimap;
          }

          public int width() {
            return modules.minimapSize.getInt();
          }

          public int height() {
            return modules.minimapSize.getInt()+6+(modules.minimapCoords.get()?12:0)+(modules.minimapBiome.get()?12:0);
          }

          public void draw(Canvas c) {
            if(diorama!=null&&mapPlatform.inWorld()&&map.fairPlay().minimapAllowed()){
              int size=modules.minimapSize.getInt();
              minimapTarget.draw(OrnitheClient.this,c,size,modules.minimapShape.get()==TrsModules.MapShape.ROUND,modules.minimapOpacity.getInt(),()->diorama.draw(c,0,0,size,size,mapPlatform.x(),mapPlatform.y(),mapPlatform.z(),modules.minimapZoom.get().pixelsPerBlock(),diorama.autoLayer()));
              dev.theredstonee.trsclient.core.ui.SelectionStyle.rounded(c,size/2-2,size/2-2,5,5,2,Theme.get().accent);
              if(modules.minimapCompass.get()){for(int i=0;i<4;i++){int[] cx={size/2,size-7,size/2,3},cy={3,size/2,size-10,size/2};c.text(new String[]{"N","E","S","W"}[i],cx[i],cy[i],0xfff4f7fc,true);}}
              int textY=size+4;if(modules.minimapCoords.get()){c.text((int)mapPlatform.x()+", "+(int)mapPlatform.y()+", "+(int)mapPlatform.z(),3,textY,modules.minimap.textColor.argb(),true);textY+=12;}if(modules.minimapBiome.get())c.text(c.clip(mapPlatform.biome(),size-6),3,textY,modules.minimap.textColor.argb(),true);
            }
          }
        });
  }

  public void openWorldMap(Screen parent) {
    if (mapPlatform == null || !mapPlatform.inWorld() || !map.fairPlay().minimapAllowed()) return;
    mc.openScreen(new CoreScreen(new OrnitheDioramaUi(this,()->mc.openScreen(parent)),false));
  }

  public void clickSound() {
    if (mc != null && mc.getSoundManager() != null)
      mc.getSoundManager()
          .play(
              net.minecraft.client.sound.instance.SimpleSoundInstance.of(
                  new net.minecraft.resource.Identifier("minecraft:gui.button.press"), 1f));
  }

  public void openMenu(Screen parent) {
    mc.openScreen(new CoreScreen(new ModMenu(new OrnitheMenuHost(this, parent)), false));
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
                return "Minecraft 1.8.9 · TFL Client · Experimental";
              }

              public String clientVersion() {
                return version();
              }

              public void playClick() {
                clickSound();
              }

              public void narrate(String text) {}

              public boolean animated() {
                return modules.titleAnimated.get();
              }

              public boolean simpleAnimation() {
                return true;
              }
            }),
        true);
  }

  private void tick() {
    if(mc.screen==null)dev.theredstonee.trsclient.core.ui.background.MenuBackground.shared().close();
    synchronizeKeys();
    mapPlatform.updateIdentity();
    // Diorama owns terrain capture and tile work; the old engine remains a data migration reader.
    if(diorama==null)diorama=new OrnitheDiorama(this);
    diorama.tick();
    notes.tick(System.currentTimeMillis());
    boolean noteKey = OrnitheKeys.isDown(modules.notes.openKey.get());
    if (noteKey && !notesDown && mc.screen == null && capabilities.active(modules.notes.notes)) {
      dev.theredstonee.trsclient.core.notes.Notes.requestOpen();
      openMenu(null);
    }
    notesDown = noteKey;
    navigation.screenshots.tick();
    String profile = modules.serverProfiles.update(mapPlatform.worldKey());
    if (profile != null) {
      save();
      if (modules.comfort.profilesNotify.get()) mapPlatform.message(profile);
    }
    emotes.tick();
    pvp.tick();
    heads.tick();
    network.tick();
    seasonal.tick();
    capes.tick();
    privacy.tick();
    automation.tick();
    refill.tick();
    comfort.tick();
    vanillaHuds.tick();
    chat.tick();
    redstone.tick();
    circuits.tick();
    cinematic.tick();
    waypointOverlay.tick();
    panorama.tick();
    while (emoteKey.consumeClick()) emotes.open(mc.screen);
    while (mapKey.consumeClick())
      if (capabilities.active(modules.worldMap)) openWorldMap(mc.screen);
    while (menuKey.consumeClick()) {
      if (mc.screen instanceof CoreScreen cs && !cs.home) mc.openScreen(null);
      else openMenu(mc.screen);
    }
    boolean blocked =
        mc.world == null || mc.player == null || mc.screen != null || mc.player.getHealth() <= 0;
    if (blocked
        || lastWorld != mc.world
        || lastSprintCode != mc.options.sprintKey.getKeyCode()
        || lastSneakCode != mc.options.sneakKey.getKeyCode()) {
      sprint.reset();
      sneak.reset();
      sneakPresses = 0;
      sneakDown = false;
      sprintPresses = 0;
      sprintDown = false;
    }
    sprint.update(sprintPresses, modules.toggleSprint.isEnabled(), blocked);
    sprintPresses = 0;
    sneak.update(sneakPresses, modules.toggleSneak.isEnabled(), blocked);
    sneakPresses = 0;
    extendedHuds.tick();
    if(lastWorld!=mc.world&&mc.world!=null&&worldLoadNanos!=0){lastWorldLoadMs=(System.nanoTime()-worldLoadNanos)/1e6;worldLoadNanos=0;}
    if(lastWorld!=mc.world&&miniPlayer!=null)miniPlayer.close();
    lastWorld = mc.world;
    lastSprintCode = mc.options.sprintKey.getKeyCode();
    lastSneakCode = mc.options.sneakKey.getKeyCode();
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
    latency.frame();
    cinematic.frame();
    panorama.frame();
    if (!cinematic.detached()) freelook.frame();
    performance.frame();
    borderless.frame();
    long now = System.nanoTime();
    if (lastFrame != 0) frameNanos[frameCount++ % frameNanos.length] = now - lastFrame;
    lastFrame = now;
    boolean active =
        modules.zoom.isEnabled()
            && mc.world != null
            && mc.player != null
            && mc.screen == null
            && zoomKey.isPressed();
    boolean blocked =
        !modules.zoom.isEnabled()
            || mc.world == null
            || mc.player == null
            || mc.player.getHealth() <= 0
            || mc.screen != null;
    zoom.frame(
        active && !blocked, blocked, modules.zoomFactor.get(), modules.zoomSmooth.get(), now);
  }

  /**
   * Native press/release edges retain taps shorter than a client tick and ignore repeat key-downs.
   */
  public void keyState(int key, boolean down) {
    if (!ready) return;
    if (key == mc.options.sneakKey.getKeyCode()) {
      if (down && !sneakDown && mc.world != null && mc.player != null && mc.screen == null)
        sneakPresses++;
      sneakDown = down;
    }
    if (key != mc.options.sprintKey.getKeyCode()) return;
    if (down && !sprintDown && mc.world != null && mc.player != null && mc.screen == null)
      sprintPresses++;
    sprintDown = down;
  }

  public boolean sprintHeld(KeyBinding key) {
    if (ready && key == mc.options.sneakKey)
      return modules.toggleSneak.isEnabled()
          && sneak.active()
          && mc.screen == null
          && mc.world != null;
    return ready
        && key == mc.options.sprintKey
        && modules.toggleSprint.isEnabled()
        && sprint.active()
        && mc.screen == null
        && mc.world != null;
  }

  public void synchronizeKeys() {
    if (mc == null || zoomKey == null || mapKey == null) return;
    if (!Objects.equals(sharedZoomKey, modules.zoomKey.get())) {
      zoomKey.setKeyCode(OrnitheKeys.code(modules.zoomKey.get()));
      sharedZoomKey = modules.zoomKey.get();
      KeyBinding.resetMapping();
    } else if (nativeZoomKey != zoomKey.getKeyCode()) {
      modules.zoomKey.set(OrnitheKeys.name(zoomKey.getKeyCode()));
      sharedZoomKey = modules.zoomKey.get();
    }
    if (!Objects.equals(sharedMapKey, modules.worldMapKey.get())) {
      mapKey.setKeyCode(OrnitheKeys.code(modules.worldMapKey.get()));
      sharedMapKey = modules.worldMapKey.get();
      KeyBinding.resetMapping();
    } else if (nativeMapKey != mapKey.getKeyCode()) {
      modules.worldMapKey.set(OrnitheKeys.name(mapKey.getKeyCode()));
      sharedMapKey = modules.worldMapKey.get();
    }
    nativeZoomKey = zoomKey.getKeyCode();
    nativeMapKey = mapKey.getKeyCode();
  }

  public int wheel(int amount) {
    if (ready && mc.screen == null && cinematic.controller.scroll(cinematic, Math.signum(amount)))
      return 0;
    if (ready
        && mc.screen == null
        && modules.zoomScroll.get()
        && zoom.isActive()
        && modules.zoom.isEnabled()) {
      zoom.scroll(Math.signum(amount));
      return 0;
    }
    return amount;
  }

  private void installCamera() {
    var state = dev.theredstonee.trsclient.ornithe.capability.CapabilityRegistry.State.LIMITED;
    capabilities.connect(
        modules.zoom,
        state,
        "Native FOV, scroll, mouse slowdown, cinematic smoothing and configurable binding",
        modules.zoom.settings().stream().map(s -> s.key()).toList(),
        "GameRendererMixin.getFov",
        "MouseMixin.tick",
        "MinecraftMixin.scroll",
        "GameRendererMixin.cinematic");
    capabilities.connect(
        modules.noHurtCam,
        state,
        "Cancels native hurt-camera transform; restores automatically when disabled",
        List.of(),
        "GameRendererMixin.applyHurtCam");
    capabilities.connect(
        modules.fullbright,
        state,
        "Native lightmap gamma read; original option is never overwritten",
        List.of("level"),
        "GameRendererMixin.updateLightMap");
  }

  public void click(int code) {
    if (ready && mc.world != null && mc.player != null && mc.screen == null) {
      long now = System.nanoTime() / 1_000_000;
      if (code == -100) {
        left.record(now);
        if (emotes != null) emotes.attack();
      }
      if (code == -99) right.record(now);
    }
  }

  private void createHud() {
    miniPlayer=new OrnitheMiniPlayer(this);hudItems.add(miniPlayer);
    capabilities.connect(modules.miniPlayer,dev.theredstonee.trsclient.ornithe.capability.CapabilityRegistry.State.EXPERIMENTAL,"Live native player, cached depth framebuffer and draw-only equipment filters",modules.miniPlayer.settings().stream().map(s->s.key()).toList(),"OrnitheMiniPlayer","EntityRenderDispatcher.render");
    hudItems.add(item(modules.fps, () -> fps));

    hudItems.add(item(modules.cps, () -> cps));
    hudItems.add(item(modules.toggleSprint, () -> sprintLabel));
    hudItems.add(new OrnitheKeystrokes(this));
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

  public void drawHud() { drawHud(1); }
  public void drawHud(float partial) {
    hudPartialTick=partial;
    if (!ready || mc.world == null || mc.player == null){if(miniPlayer!=null)miniPlayer.close();return;}
    if(mc.options.hideGui||mc.screen instanceof CoreScreen cs&&!cs.ui.overlay()){
        if(miniPlayer!=null&&modules.miniPlayer.shouldDraw(false,true,true,mc.options.hideGui,mc.screen!=null,true))drawMiniOnly();return;
    }
    if (mc.screen instanceof CoreScreen cs && !cs.ui.overlay()) return;
    Window win = window();
    canvas.begin(win.getWidth(), win.getHeight(), win.getScale());
    try {
      for (HudItem item : hudItems) {
        HudModule m = item.module();
        if (!m.isEnabled()) continue;
        float s = (float) m.scale.get();
        int x = HudLayout.resolveX(m.position(), Math.round(item.width() * s), win.getWidth()),
            y = HudLayout.resolveY(m.position(), Math.round(item.height() * s), win.getHeight());
        if(x+item.width()*s<0||y+item.height()*s<0||x>=win.getWidth()||y>=win.getHeight())continue;
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
      dev.theredstonee.trsclient.core.panorama.PanoramaToast.draw(
          canvas,
          win.getWidth(),
          win.getHeight(),
          dev.theredstonee.trsclient.core.panorama.Panorama.get().toast(System.currentTimeMillis()),
          "TFL Client");
      pvp.draw(canvas, win.getWidth(), win.getHeight());
      redstone.draw(canvas, win.getWidth(), win.getHeight());
      comfort.draw(canvas, win.getWidth(), win.getHeight());
      seasonal.draw(canvas, win.getWidth(), win.getHeight());
      circuits.draw(canvas, win.getWidth(), win.getHeight());
      waypointOverlay.draw(canvas, win.getWidth(), win.getHeight());
      navigation.screenshots.render(canvas, win.getWidth(), win.getHeight(), -1, -1, false);
    } finally {
      canvas.end();
    }
  }
  /** F1 suppresses vanilla's entire GUI callback, so draw this explicit opt-in overlay separately. */
  public void drawHiddenMini(float partial){
    if(!ready||mc.world==null||mc.player==null||!mc.options.hideGui||mc.screen!=null||modules.miniPlayer.hideF1.get())return;
    hudPartialTick=partial;Window w=window();int mode=org.lwjgl.opengl.GL11.glGetInteger(org.lwjgl.opengl.GL11.GL_MATRIX_MODE);
    canvas.begin(w.getWidth(),w.getHeight(),w.getScale());
    try{
      GlStateManager.matrixMode(org.lwjgl.opengl.GL11.GL_PROJECTION);GlStateManager.pushMatrix();GlStateManager.loadIdentity();GlStateManager.ortho(0,w.getWidth(),w.getHeight(),0,-1000,1000);
      GlStateManager.matrixMode(org.lwjgl.opengl.GL11.GL_MODELVIEW);GlStateManager.loadIdentity();GlStateManager.disableDepthTest();GlStateManager.depthMask(false);drawMiniOnly();
    }finally{GlStateManager.matrixMode(org.lwjgl.opengl.GL11.GL_PROJECTION);GlStateManager.popMatrix();GlStateManager.matrixMode(org.lwjgl.opengl.GL11.GL_MODELVIEW);canvas.end();GlStateManager.matrixMode(mode);}
  }
  private void drawMiniOnly(){Window w=window();canvas.begin(w.getWidth(),w.getHeight(),w.getScale());try{var m=modules.miniPlayer;float s=m.scale.getFloat();int x=HudLayout.resolveX(m.position(),Math.round(miniPlayer.width()*s),w.getWidth()),y=HudLayout.resolveY(m.position(),Math.round(miniPlayer.height()*s),w.getHeight());if(x+miniPlayer.width()*s<=0||y+miniPlayer.height()*s<=0||x>=w.getWidth()||y>=w.getHeight())return;canvas.push();try{canvas.translate(x,y);canvas.scale(s);miniPlayer.draw(canvas);}finally{canvas.pop();}}finally{canvas.end();}}
}
