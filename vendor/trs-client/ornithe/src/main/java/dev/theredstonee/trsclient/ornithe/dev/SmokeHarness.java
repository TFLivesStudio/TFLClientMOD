package dev.theredstonee.trsclient.ornithe.dev;

import com.google.gson.*;
import dev.theredstonee.trsclient.core.ui.menu.*;
import dev.theredstonee.trsclient.core.ui.title.TitleUi;
import dev.theredstonee.trsclient.ornithe.*;
import java.nio.file.*;
import java.util.*;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.Screenshot;
import net.minecraft.client.options.KeyBinding;
import net.minecraft.world.WorldSettings;
import net.minecraft.world.gen.WorldGeneratorType;

/**
 * Development-only, excluded from all distributable archives. Tick-driven real-client integration
 * checks.
 */
public final class SmokeHarness implements Runnable {
  private final OrnitheClient c;
  private final Path out;
  private final JsonObject report = new JsonObject();
  private final JsonArray checks = new JsonArray();
  private int tick, stage, wait, inputFrame;
  private float normalFov;
  private long mapUploads;
  private boolean fogZoomed;private int mapProbeStage;private long miniBefore;private Object playerBefore;private int dimensionBefore;
  private long screenshotDeadline;
  private java.nio.file.Path nativeScreenshot;
  private long colorPasses, panoramaFaces;
  private double playerX, playerZ;
  private boolean hiddenBefore;

  private long posesBefore;
  private int perspectiveBefore;
  private net.minecraft.client.render.model.entity.HumanoidModel emoteModel;

  private float fov() throws Exception {
    var method =
        java.util.Arrays.stream(c.mc.gameRenderer.getClass().getDeclaredMethods()).filter(m->m.getReturnType()==float.class&&java.util.Arrays.equals(m.getParameterTypes(),new Class<?>[]{float.class,boolean.class})).findFirst().orElseThrow();
    method.setAccessible(true);
    return (float) method.invoke(c.mc.gameRenderer, 0f, true);
  }

  private final long started = System.nanoTime();
  private final boolean restart;

  public SmokeHarness(OrnitheClient c) throws Exception {
    this.c = c;
    out = Paths.get(System.getProperty("tflcore.ornithe.report", "smoke"));
    Files.createDirectories(out);
    restart = Boolean.getBoolean("tflcore.ornithe.restart");
    report.addProperty("java", System.getProperty("java.version"));
    report.addProperty("lwjgl3", FabricLoader.getInstance().isModLoaded("legacy-lwjgl3"));
    report.addProperty("argentum", FabricLoader.getInstance().isModLoaded("argentum"));
    report.addProperty(
        "renderer", org.lwjgl.opengl.GL11.glGetString(org.lwjgl.opengl.GL11.GL_RENDERER));
    report.addProperty("version", c.version());
    report.addProperty("restart", restart);
    report.add("checks", checks);
  }


  private int videoProbeStage;
  private long videoProbeAt;
  private final com.google.gson.JsonArray videoEvidence=new com.google.gson.JsonArray();
  private boolean videoProbe(){
    var bg=dev.theredstonee.trsclient.core.ui.background.MenuBackground.shared();
    var m=bg.metrics();long now=System.nanoTime();
    if(videoProbeAt==0){if(m.framesPresented<2)return false;videoProbeAt=now;shot("depth-video-start");return false;}
    double elapsed=(now-videoProbeAt)/1e9;
    double duration=videoProbeStage==0?20:videoProbeStage==1||videoProbeStage==2||videoProbeStage==5?3:1;
    if(elapsed<duration||videoProbeStage==0&&m.loops<1&&elapsed<45)return false;
    var item=new com.google.gson.JsonObject();item.addProperty("scenario",videoProbeStage);item.add("metrics",new com.google.gson.Gson().toJsonTree(m));videoEvidence.add(item);
    shot("depth-video-"+videoProbeStage);
    if(videoProbeStage==0){if(m.loops<1||m.framesPresented<20)throw new AssertionError("Native video loop/presentation did not advance");c.modules.menuBackground.set(dev.theredstonee.trsclient.core.ui.background.MenuBackground.Mode.HIGH);}
    else if(videoProbeStage==1){if(m.framesPresented<3)throw new AssertionError("Quality video did not advance");c.modules.menuBackground.set(dev.theredstonee.trsclient.core.ui.background.MenuBackground.Mode.PERFORMANCE);}
    else if(videoProbeStage==2){if(m.framesPresented<3)throw new AssertionError("Performance video did not advance");c.modules.menuBackground.set(dev.theredstonee.trsclient.core.ui.background.MenuBackground.Mode.STATIC);}
    else if(videoProbeStage==3){if(bg.videoActive())throw new AssertionError("Static preference started a decoder");c.modules.menuBackground.set(dev.theredstonee.trsclient.core.ui.background.MenuBackground.Mode.OFF);}
    else if(videoProbeStage==4){if(bg.videoActive())throw new AssertionError("Off preference started a decoder");bg.close();c.modules.menuBackground.set(dev.theredstonee.trsclient.core.ui.background.MenuBackground.Mode.BALANCED);}
    else {if(m.framesPresented<3)throw new AssertionError("Video did not restart");return true;}
    videoProbeStage++;videoProbeAt=now;return false;
  }
  private void check(String name, boolean ok) {
    JsonObject r = new JsonObject();
    r.addProperty("name", name);
    r.addProperty("passed", ok);
    checks.add(r);
    if (!ok) throw new AssertionError(name);
  }

  private void shot(String name) {
    Screenshot.take(out.toFile(), name + ".png", c.mc.width, c.mc.height, c.mc.getRenderTarget());
  }

  private UnifiedSmoke unified;
  // Diagnostics heartbeat: prints probe state with timestamps and persists a small JSON pulse so
  // the smoke driver can enforce an inactivity deadline separately from the total runtime budget.
  // Daemon-only and read-only with respect to the client; it never touches game state.
  private Thread pulse;
  private static final java.time.format.DateTimeFormatter PULSE_STAMP =
      java.time.format.DateTimeFormatter.ISO_INSTANT;

  private void startPulse() {
    if (pulse != null) return;
    pulse =
        new Thread(
            () -> {
              while (true) {
                try {
                  if (unified != null) {
                    long elapsed = (System.nanoTime() - unified.startNanos()) / 1_000_000L;
                    String stamp = PULSE_STAMP.format(java.time.Instant.now());
                    System.out.println(
                        "[TFL unified pulse] phase="
                            + unified.phaseNumber()
                            + " checks="
                            + unified.checkCount()
                            + " last="
                            + unified.lastOperation()
                            + " at "
                            + elapsed
                            + " ms");
                    JsonObject item = new JsonObject();
                    item.addProperty("type", "probe-pulse");
                    item.addProperty("phase", unified.phaseNumber());
                    item.addProperty("checks", unified.checkCount());
                    item.addProperty("lastOp", unified.lastOperation());
                    item.addProperty("elapsedMs", elapsed);
                    item.addProperty("stampIso", stamp);
                    java.nio.file.Path tmp =
                        out.resolve("probe-pulse.json.tmp" + Long.toHexString(System.nanoTime()));
                    Files.write(
                        tmp,
                        new GsonBuilder().create().toJson(item).getBytes(java.nio.charset.StandardCharsets.UTF_8));
                    Files.move(
                        tmp,
                        out.resolve("probe-pulse.json"),
                        java.nio.file.StandardCopyOption.REPLACE_EXISTING);
                  }
                } catch (Throwable diagnosticOnlyIfAvailable) {
                  // A pulse failure must never influence the probe outcome.
                }
                try {
                  Thread.sleep(30_000L);
                } catch (InterruptedException interrupted) {
                  return;
                }
              }
            },
            "tfl-smoke-pulse");
    pulse.setDaemon(true);
    pulse.start();
  }

  @Override
  public void run() {
    if(Boolean.getBoolean("tflcore.unified.smoke")){if(unified==null)unified=new UnifiedSmoke(c,out);startPulse();unified.run();return;}
    try {
      tick++;
      if (System.nanoTime() - started > 300_000_000_000L)
        throw new AssertionError("client timeout");
      if (--wait > 0) return;
      switch (stage) {
        case 0 -> {
          c.mc.options.pauseOnUnfocus = false;
          c.modules.worldMapTilt.set(0);c.modules.worldMapRotation.set(0);if(!restart)c.modules.miniPlayer.reset();
          if(Boolean.getBoolean("tflcore.ui.smoke.large")) {
            c.mc.options.guiScale=4;
            c.mc.openScreen(c.title());
          }
          check(
              "shared main menu", c.mc.screen instanceof CoreScreen cs && cs.ui instanceof TitleUi);
          if (restart) {
            check(
                "preferences loaded after process restart",
                c.modules.coords.isEnabled()
                    && c.modules.crosshair.isEnabled()
                    && c.modules.crosshairSize.get() == 7);
            check("Mini Player preferences survive process restart",c.modules.miniPlayer.opacity.getInt()==65&&c.modules.miniPlayer.angle.get()==dev.theredstonee.trsclient.core.hud.miniplayer.MiniPlayerModule.Angle.SIDE&&Math.abs(c.modules.miniPlayer.position().offsetX-.123)<1e-8);
            shot("restart-title");
            finish(null);
            return;
          }
          c.modules.menuBackground.set(dev.theredstonee.trsclient.core.ui.background.MenuBackground.Mode.BALANCED);
          wait = 25;
          stage++;
        }
        case 1 -> {
          if(!videoProbe())return;report.add("videoEvidence",videoEvidence);
          check("bundled blue video presents decoded frames",dev.theredstonee.trsclient.core.ui.background.MenuBackground.shared().videoFramePresented());
          shot("title");
          KeyBinding.click(c.menuKey.getKeyCode());
          wait = 15;
          stage++;
        }
        case 2 -> {
          check(
              "registered menu key opens common settings",
              c.mc.screen instanceof CoreScreen cs && cs.ui instanceof ModMenu);
          shot("settings");
          c.mc.openScreen(null);
          check(
              "null-screen return uses shared title",
              c.mc.screen instanceof CoreScreen cs && cs.ui instanceof TitleUi);
          new OrnitheMenuHost(c, null).openHudEditor();
          wait = 15;
          stage++;
        }
        case 3 -> {
          check(
              "shared HUD editor",
              c.mc.screen instanceof CoreScreen cs && cs.ui instanceof HudEditor);
          shot("editor");
          c.mc.openScreen(null);
          c.mc.startGame(
              "tfl-depth-polish-smoke",
              "TFL Ornithe Smoke",
              new WorldSettings(
                      189L, WorldSettings.GameMode.CREATIVE, false, false, WorldGeneratorType.FLAT)
                  .enableCommands());
          wait = 40;
          stage++;
        }
        case 4 -> {
          if (c.mc.world == null || c.mc.player == null) {
            wait = 10;
            return;
          }
          c.mc.openScreen(null);
          check("world entered", true);
          for (var m : c.modules.registry.all())
            if (c.capabilities.runnable(m) && m.userToggleable()) {
              m.setEnabled(false);
              check(m.id() + " disabled", !m.isEnabled());
              m.setEnabled(true);
              check(m.id() + " enabled", m.isEnabled());
            }
          c.modules.crosshairSize.set(7);
          c.save();
          c.config.flush();
          KeyBinding.click(-100);
          KeyBinding.click(-99);
          check(
              "CPS hook both buttons",
              c.left.count(System.nanoTime() / 1_000_000) > 0
                  && c.right.count(System.nanoTime() / 1_000_000) > 0);
          c.modules.zoomSmooth.set(false);
          normalFov = fov();
          inputFrame = c.frameCount;
          new java.awt.Robot().keyPress(java.awt.event.KeyEvent.VK_CONTROL);
          new java.awt.Robot().keyPress(java.awt.event.KeyEvent.VK_V);
          wait = 8;
          stage++;
        }
        case 5 -> {
          // World creation can queue many client ticks before another input/render frame.
          if (c.frameCount - inputFrame < 5) {
            wait = 1;
            return;
          }
          check("native frame applies zoom", c.zoom.factor() > 1);
          check("native FOV return hook", fov() < normalFov * .75f);
          new java.awt.Robot().keyRelease(java.awt.event.KeyEvent.VK_CONTROL);
          check("native crosshair eligibility captured", c.crosshairVisible);
          shot("world-zoom");
          new java.awt.Robot().keyRelease(java.awt.event.KeyEvent.VK_V);
          check("physical sprint key toggles latch", c.sprint.active());
          check("native sprint binding held", c.mc.options.sprintKey.isPressed());
          c.modules.toggleSprint.setEnabled(false);

          wait = 15;
          stage++;
        }
        case 6 -> {
          check("disable restores native sprint binding", !c.mc.options.sprintKey.isPressed());
          c.modules.toggleSprint.setEnabled(true);
          check("zoom released restores FOV divisor", c.zoom.factor() == 1);
          shot("world");
          KeyBinding.click(c.menuKey.getKeyCode());
          wait = 10;
          stage++;
        }
        case 7 -> {
          check("menu opens in world", c.mc.screen instanceof CoreScreen);
          check("sprint resets in GUI", !c.sprint.active());
          shot("world-settings");
          c.mc.openScreen(null);
          KeyBinding.set(c.mc.options.sprintKey.getKeyCode(), true);
          KeyBinding.set(c.mc.options.sprintKey.getKeyCode(), false);
          c.frameCount = 0;
          wait = 200;
          stage++;
        }
        case 8 -> {
          check("sprint tap shorter than one client tick retained", c.sprint.active());
          check("frames recorded", c.frameCount > 0);
          c.save();
          c.config.flush();
          check("preferences written", Files.isRegularFile(c.config.file()));
          nativeFeatures();
          perspectiveBefore = c.mc.options.perspective;
          c.modules.emoteCamera.set(
              dev.theredstonee.trsclient.core.module.TrsModules.EmoteCamera.BACK);
          check(
              "local emote starts without online services",
              c.emotes.controller.play(
                      c.emotes.controller.all().get(0).id(), System.currentTimeMillis())
                  == dev.theredstonee.trsclient.core.emote.EmoteController.Result.PLAYED);
          emoteModel = new net.minecraft.client.render.model.entity.HumanoidModel();
          posesBefore = c.emotes.appliedPoses;
          wait = 8;
          stage++;
        }
        case 9 -> {
          emoteModel.setupAnimation(0, 0, 1, 0, 0, .0625f, c.mc.player);
          check("native humanoid emote pose applied", c.emotes.appliedPoses > posesBefore);
          check("emote selects third person", c.mc.options.perspective == 1);
          shot("local-emote");
          c.emotes.attack();
          wait = 10;
          stage++;
        }
        case 10 -> {
          check(
              "attack cancels local emote",
              !c.emotes.controller.selfPlaying(System.currentTimeMillis()));
          check(
              "emote restores previous perspective", c.mc.options.perspective == perspectiveBefore);
          emoteModel.setupAnimation(0, 0, 1, 0, 0, .0625f, c.mc.player);
          var reference = new net.minecraft.client.render.model.entity.HumanoidModel();
          reference.setupAnimation(0, 0, 1, 0, 0, .0625f, c.mc.player);
          check(
              "humanoid restores vanilla pose after emote",
              Math.abs(emoteModel.rightArm.rotationX - reference.rightArm.rotationX) < .0001f
                  && Math.abs(emoteModel.rightArm.x - reference.rightArm.x) < .0001f);
          colorPasses = c.colors.applied;
          c.modules.colors.setEnabled(true);
          c.modules.colorSaturation.set(0);
          inputFrame = c.frameCount;
          wait = 10;
          stage++;
        }
        case 11 -> {
          if (c.frameCount - inputFrame < 3) {
            wait = 1;
            return;
          }
          check("native color post-process executes", c.colors.applied > colorPasses);
          shot("world-grayscale");
          c.modules.colorSaturation.set(100);
          c.modules.colors.setEnabled(false);
          check(
              "offline circuit recipes available",
              dev.theredstonee.trsclient.core.circuit.CircuitLibrary.get().all().size() >= 3);
          check(
              "circuit service clients are absent",
              dev.theredstonee.trsclient.core.circuit.Circuits.get().submissions() == null);
          var circuit =
              dev.theredstonee.trsclient.core.circuit.CircuitLibrary.get().byId("rs_latch");
          dev.theredstonee.trsclient.core.circuit.Circuits.get()
              .placeAt(
                  circuit,
                  (int) Math.floor(c.mc.player.x) + 2,
                  8,
                  (int) Math.floor(c.mc.player.z),
                  0,
                  false);
          wait = 10;
          stage++;
        }
        case 12 -> {
          var circuits = dev.theredstonee.trsclient.core.circuit.Circuits.get();
          check(
              "native circuit checker sees missing template blocks",
              circuits.check() != null && circuits.check().missing() > 0);
          shot("circuit-template");
          circuits.remove();
          playerX = c.mc.player.x;
          playerZ = c.mc.player.z;
          c.cinematic.controller.toggleFreecam(c.cinematic);
          check(
              "freecam detaches the native camera",
              c.cinematic.detached() && c.mc.getCamera() != c.mc.player);
          new java.awt.Robot().keyPress(java.awt.event.KeyEvent.VK_W);
          inputFrame = c.frameCount;
          wait = 10;
          stage++;
        }
        case 13 -> {
          if (c.frameCount - inputFrame < 5) {
            wait = 1;
            return;
          }
          new java.awt.Robot().keyRelease(java.awt.event.KeyEvent.VK_W);
          check(
              "freecam physical movement leaves player in place",
              Math.hypot(c.mc.player.x - playerX, c.mc.player.z - playerZ) < .01);
          check(
              "freecam physical movement moves only the camera",
              Math.hypot(c.mc.getCamera().x - playerX, c.mc.getCamera().z - playerZ) > .05);
          shot("local-freecam");
          c.cinematic.controller.toggleFreecam(c.cinematic);
          check("freecam restores the native player camera", c.mc.getCamera() == c.mc.player);
          hiddenBefore = c.mc.options.hideGui;
          panoramaFaces = c.panorama.faces;
          dev.theredstonee.trsclient.core.panorama.Panorama.get().request(true, true);
          wait = 20;
          stage++;
        }
        case 14 -> {
          var panorama = dev.theredstonee.trsclient.core.panorama.Panorama.get();
          if (panorama.state() != dev.theredstonee.trsclient.core.panorama.Panorama.State.IDLE) {
            wait = 10;
            return;
          }
          check("panorama captures six native world faces", c.panorama.faces - panoramaFaces == 6);
          check(
              "panorama encodes the local image",
              panorama.lastImage() != null && Files.isRegularFile(panorama.lastImage()));
          check(
              "panorama restores HUD and camera",
              c.mc.options.hideGui == hiddenBefore && c.mc.getCamera() == c.mc.player);
          check(
              "color disable releases native post-chain",
              !c.modules.colors.isEnabled() && !c.colors.allocated());
          var point =
              new dev.theredstonee.trsclient.core.waypoint.Waypoint(
                  "Native fixture", -17, 64, -33, c.mapPlatform.dimension(), 0x3D7BFF);
          c.waypoints.add(c.mapPlatform.worldKey(), point);
          c.waypoints.saveIfDirty();
          var loaded =
              new dev.theredstonee.trsclient.core.waypoint.WaypointStore(c.waypoints.file());
          loaded.load();
          check(
              "native world waypoints persist negative coordinates and dimension",
              loaded.all(c.mapPlatform.worldKey()).stream()
                  .anyMatch(
                      w ->
                          w.name.equals("Native fixture")
                              && w.x == -17
                              && w.z == -33
                              && w.dimension.equals(c.mapPlatform.dimension())));
          c.waypoints.remove(c.mapPlatform.worldKey(), point);
          c.waypoints.saveIfDirty();
          mapUploads = c.map.textures().uploads();
          KeyBinding.click(c.mapKey.getKeyCode());
          wait = 15;
          stage++;
        }
        case 15 -> {
          if(!fogZoomed&&c.mc.screen instanceof CoreScreen cs){
            cs.ui.mouseScrolled(cs.width/2.0,cs.height/2.0,-3);fogZoomed=true;wait=15;return;
          }
          check(
              "native map key opens shared world map",
              c.mc.screen instanceof CoreScreen cs
                  && cs.ui instanceof dev.theredstonee.trsclient.core.map.WorldMapUi);
          check(
              "world map uploads received region textures",
              c.map.textures().regionTextureCount() > 0
                  && c.map.textures().uploads() >= mapUploads);
          report.addProperty("miniPlayerFailure",c.miniPlayer.state.failure);report.addProperty("miniPlayerUpdates",c.miniPlayer.state.updates);report.addProperty("miniComposites",c.miniPlayer.composites);check("Mini Player native framebuffer",c.miniPlayer.state.failure.isEmpty());check("Mini Player drew native model",c.miniPlayer.composites>0);check("Mini Player framebuffer has visible model pixels",c.miniPlayer.visiblePixels()>100);
          var mapUi=(dev.theredstonee.trsclient.core.map.WorldMapUi)((CoreScreen)c.mc.screen).ui;
          var volume=mapUi.volumeRenderer();
          check("bounded native 3D volume available",volume.failure().isEmpty());
          if(volume.displayed()==null){wait=5;return;}
          if(mapProbeStage<3){
            double expected=mapProbeStage==0?0:mapProbeStage==1?30:55;
            if(Math.abs(volume.displayed().tilt-expected)>.1){wait=5;return;}
            shot("depth-map-"+mapProbeStage);var camera=volume.displayed();
            double[] at=mapUi.testWorldAt(camera.width/2,camera.height/2);
            check("perspective inverse center "+mapProbeStage,Math.abs(at[0]-camera.cx)<.01&&Math.abs(at[1]-camera.cz)<.01);
            report.addProperty("volumeBackend",volume.backend());report.addProperty("volumeFrames",volume.renderedFrames());report.addProperty("volumeAverageMs",volume.renderNanos()/1e6/Math.max(1,volume.renderedFrames()));
            mapProbeStage++;if(mapProbeStage<3){c.modules.worldMapTilt.set(mapProbeStage==1?30:55);wait=30;return;}
          }
          shot("world-map");
          c.mc.openScreen(null);
          String name =
              java.time.LocalDateTime.now()
                      .format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd_HH.mm.ss"))
                  + "_189.png";
          nativeScreenshot = c.mc.gameDir.toPath().resolve("screenshots").resolve(name);
          Screenshot.take(c.mc.gameDir, name, c.mc.width, c.mc.height, c.mc.getRenderTarget());
          screenshotDeadline=System.nanoTime()+10_000_000_000L;
          wait = 20;
          stage++;
        }
        case 16 -> {
          var toastFile=c.navigation.screenshots.toast().file();
          boolean watched=toastFile!=null&&nativeScreenshot.toAbsolutePath().normalize().equals(toastFile.toAbsolutePath().normalize());
          if((!Files.isRegularFile(nativeScreenshot)||!watched)&&System.nanoTime()<screenshotDeadline){wait=5;return;}
          check("native screenshot file written", Files.isRegularFile(nativeScreenshot));
          check(
              "local screenshot watcher updates toast",
              c.navigation.screenshots.toast().file() != null
                  && nativeScreenshot
                      .toAbsolutePath()
                      .normalize()
                      .equals(
                          c.navigation.screenshots.toast().file().toAbsolutePath().normalize()));
          c.navigation.captures(null);
          wait = 15;
          stage++;
        }
        case 17 -> {
          check(
              "shared local gallery opens natively",
              c.mc.screen instanceof CoreScreen cs
                  && cs.ui instanceof dev.theredstonee.trsclient.core.ui.clips.ClipsUi);
          shot("local-gallery");
          c.mc.openScreen(c.navigation.pause());wait=15;stage++;
        }
        case 18 -> {
          check("shared transparent pause navigation",c.mc.screen instanceof CoreScreen cs && cs.ui instanceof dev.theredstonee.trsclient.core.ui.menus.ActionListUi);
          shot("pause");
          c.mc.openScreen(null);
          for(var module:c.modules.registry.all())if(module instanceof dev.theredstonee.trsclient.core.module.HudModule)module.setEnabled(module==c.modules.miniPlayer);
          c.modules.miniPlayer.setEnabled(true);c.modules.miniPlayer.position().offsetX=.02;c.modules.miniPlayer.position().offsetY=.02;
          c.modules.miniPlayer.angle.set(dev.theredstonee.trsclient.core.hud.miniplayer.MiniPlayerModule.Angle.FRONT);c.mc.player.sendChat("/clear @p");
          stage++;wait=15;
        }
        case 19 -> {
          MiniGlProbe.verify(c);check("Mini Player restores native GL and world camera state",true);
          check("production Paper Doll contains visible pixels",c.miniPlayer.visiblePixels()>100);shot("depth-mini-front");
          c.mc.player.sendChat("/replaceitem entity @p slot.armor.head minecraft:diamond_helmet");
          c.mc.player.sendChat("/replaceitem entity @p slot.armor.chest minecraft:diamond_chestplate");
          c.mc.player.sendChat("/replaceitem entity @p slot.hotbar.0 minecraft:diamond_sword");stage++;wait=20;
        }
        case 20 -> {
          check("native Paper Doll equipment received",c.mc.player.inventory.armor[3]!=null&&c.mc.player.inventory.getSelectedItem()!=null);shot("depth-mini-equipment");
          c.modules.miniPlayer.armor.set(false);c.modules.miniPlayer.heldItems.set(false);stage++;wait=10;
        }
        case 21 -> {
          shot("depth-mini-hidden-equipment");c.modules.miniPlayer.armor.set(true);c.modules.miniPlayer.heldItems.set(true);
          playerX=c.mc.player.x;playerZ=c.mc.player.z;KeyBinding.set(c.mc.options.forwardKey.getKeyCode(),true);stage++;wait=15;
        }
        case 22 -> {
          check("Paper Doll uses moving real player",Math.abs(c.mc.player.x-playerX)+Math.abs(c.mc.player.z-playerZ)>.1);shot("depth-mini-walk");
          KeyBinding.set(c.mc.options.forwardKey.getKeyCode(),false);KeyBinding.set(c.mc.options.jumpKey.getKeyCode(),true);stage++;wait=3;
        }
        case 23 -> {
          shot("depth-mini-jump");KeyBinding.set(c.mc.options.jumpKey.getKeyCode(),false);KeyBinding.set(c.mc.options.sneakKey.getKeyCode(),true);stage++;wait=10;
        }
        case 24 -> {
          check("Paper Doll real sneak",c.mc.player.isSneaking());shot("depth-mini-sneak");KeyBinding.set(c.mc.options.sneakKey.getKeyCode(),false);c.mc.player.swingArm();stage++;wait=2;
        }
        case 25 -> {
          shot("depth-mini-attack");c.modules.emoteCamera.set(dev.theredstonee.trsclient.core.module.TrsModules.EmoteCamera.OFF);
          c.emotes.controller.play(c.emotes.controller.all().get(0).id(),System.currentTimeMillis());stage++;wait=10;
        }
        case 26 -> {
          shot("depth-mini-emote");c.emotes.controller.onAttack(System.currentTimeMillis());
          c.mc.reloadResources();stage++;wait=20;
        }
        case 27 -> {
          check("Paper Doll survives resource reload",c.miniPlayer.state.failure.isEmpty()&&c.miniPlayer.visiblePixels()>100);shot("depth-mini-resource-reload");
          c.mc.options.hideGui=true;miniBefore=c.miniPlayer.state.updates;stage++;wait=10;
        }
        case 28 -> {
          check("F1 skips native Paper Doll",c.miniPlayer.state.updates==miniBefore);c.modules.miniPlayer.hideF1.set(false);stage++;wait=10;
        }
        case 29 -> {
          check("F1 opt-in draws native Paper Doll",c.miniPlayer.state.updates>miniBefore);shot("depth-mini-F1-opt-in");c.mc.options.hideGui=false;c.modules.miniPlayer.hideF1.set(true);
          playerBefore=c.mc.player;var uuid=c.mc.player.getUuid();c.mc.getServer().executeTask(()->c.mc.getServer().getPlayerManager().get(uuid).takeDamage(net.minecraft.entity.damage.DamageSource.OUT_OF_WORLD,Float.MAX_VALUE));stage++;wait=15;
        }
        case 30 -> {
          if(c.mc.player.getHealth()>0){wait=5;return;}shot("depth-mini-death");c.mc.player.respawn();stage++;wait=30;
        }
        case 31 -> {
          if(c.mc.player==playerBefore||c.mc.player.getHealth()<=0||c.mc.screen!=null){wait=5;return;}
          check("native respawn replaced local player",c.mc.player!=playerBefore);shot("depth-mini-respawn");
          dimensionBefore=c.mc.world.dimension.getId();c.mc.options.viewDistance=3;
          int px=(int)Math.floor(c.mc.player.x),py=(int)Math.ceil(c.mc.player.y),pz=(int)Math.floor(c.mc.player.z);
          c.mc.player.sendChat("/fill "+(px-1)+" "+py+" "+pz+" "+(px+2)+" "+(py+4)+" "+pz+" minecraft:obsidian");
          c.mc.player.sendChat("/fill "+px+" "+(py+1)+" "+pz+" "+(px+1)+" "+(py+3)+" "+pz+" minecraft:portal");
          c.mc.player.sendChat("/tp @p "+(px+.5)+" "+(py+1)+" "+(pz+.5));stage++;wait=30;
        }
        case 32 -> {
          if(c.mc.world.dimension.getId()!=-1||c.mc.screen!=null){wait=5;return;}check("native Nether dimension received",true);shot("depth-mini-nether");
          var uuid=c.mc.player.getUuid();c.mc.getServer().executeTask(()->c.mc.getServer().getPlayerManager().changeDimension(c.mc.getServer().getPlayerManager().get(uuid),dimensionBefore));stage++;wait=30;
        }
        case 33 -> {
          if(c.mc.world.dimension.getId()!=dimensionBefore||c.mc.screen!=null){wait=5;return;}check("native return dimension received",true);shot("depth-mini-return");finish(null);
        }
      }
    } catch (Throwable e) {
      finish(e);
    }
  }

  /** Native client fixtures verify mapping/hook outputs, not multiplayer gameplay certification. */
  private void nativeFeatures() throws Exception {
    check(
        "map has native world identity",
        !c.map.worldKey().isEmpty() && c.map.worldKey().equals(c.mapPlatform.worldKey()));
    check("map samples received terrain", c.map.sampledTotal() > 0);
    check("map excludes unknown chunks", !c.redstone.world.loaded(29000000, 64, 29000000));
    check(
        "map reader excludes unknown chunks",
        !c.mapPlatform.isLoaded(1812500, 1812500) && !c.mapPlatform.open(1812500, 1812500));
    c.modules.autoGg.setEnabled(true);
    check(
        "legacy enabled AutoGG never grants send approval",
        !c.automation.allowed(c.modules.autoGg));
    c.modules.autoRefill.setEnabled(true);
    check("legacy enabled refill never grants inventory approval", !c.refill.active());
    check(
        "shared/native privacy masks names",
        !c.privacy.text(c.mc.player.getName()).equals(c.mc.player.getName()));
    check(
        "shared/native privacy masks address tokens",
        c.privacy.text("play.example.net").equals("*****"));
    var sword =
        new net.minecraft.item.ItemStack(
            net.minecraft.item.Item.REGISTRY.get(
                new net.minecraft.resource.Identifier("minecraft:diamond_sword")));
    sword.setDamage(100);
    check(
        "native tooltip contains shared durability",
        sword.getTooltip(c.mc.player, false).stream()
            .anyMatch(line -> line.contains(Integer.toString(sword.getMaxDamage() - 100))));
    c.modules.particleNoSmoke.set(true);
    check(
        "native particle gate suppresses configured smoke",
        c.mc.particleManager.addParticle(
                net.minecraft.entity.particle.ParticleType.SMOKE_NORMAL.getId(),
                c.mc.player.x,
                c.mc.player.y,
                c.mc.player.z,
                0,
                0,
                0)
            == null);
    c.modules.particles.setEnabled(false);
    check(
        "disabling particle gate restores native allocation",
        c.mc.particleManager.addParticle(
                net.minecraft.entity.particle.ParticleType.SMOKE_NORMAL.getId(),
                c.mc.player.x,
                c.mc.player.y,
                c.mc.player.z,
                0,
                0,
                0)
            != null);
    c.modules.particles.setEnabled(true);
    checkChat();
    checkCanvas();
    var pos =
        new net.minecraft.util.math.BlockPos(
            (int) Math.floor(c.mc.player.x) + 2, 8, (int) Math.floor(c.mc.player.z));
    var before = c.mc.world.getBlockState(pos);
    var probe = new dev.theredstonee.trsclient.core.redstone.BlockProbe();
    try {
      c.mc.world.setBlockState(
          pos,
          net.minecraft.block.Blocks.REDSTONE_WIRE
              .defaultState()
              .set(net.minecraft.block.RedstoneWireBlock.POWER, 13),
          2);
      check(
          "native redstone dust power 13",
          c.redstone.world.dustPower(pos.getX(), pos.getY(), pos.getZ()) == 13);
      check(
          "native redstone probe recognizes dust",
          c.redstone.world.probe(pos.getX(), pos.getY(), pos.getZ(), probe)
              && probe.kind == dev.theredstonee.trsclient.core.redstone.RedstoneKind.DUST
              && probe.power == 13);
      c.mc.world.setBlockState(
          pos, net.minecraft.block.Blocks.POWERED_REPEATER.getStateFromMetadata(8), 2);
      c.redstone.world.probe(pos.getX(), pos.getY(), pos.getZ(), probe);
      check(
          "native powered repeater delay",
          probe.kind == dev.theredstonee.trsclient.core.redstone.RedstoneKind.REPEATER
              && probe.powered
              && probe.delay == 3);
      c.mc.world.setBlockState(
          pos, net.minecraft.block.Blocks.POWERED_COMPARATOR.getStateFromMetadata(4), 2);
      c.redstone.world.probe(pos.getX(), pos.getY(), pos.getZ(), probe);
      check(
          "native comparator subtract mode",
          probe.kind == dev.theredstonee.trsclient.core.redstone.RedstoneKind.COMPARATOR
              && probe.subtract);
      c.modules.redstoneSignal.setEnabled(true);
      c.redstone.tools.tick(
          c.redstone.world,
          c.mc.world,
          true,
          pos.getX(),
          pos.getY(),
          pos.getZ(),
          c.mc.player.x,
          c.mc.player.y,
          c.mc.player.z);
      check("shared redstone readout consumes native states", c.redstone.tools.readout().valid);
    } finally {
      c.mc.world.setBlockState(pos, before, 2);
    }
    var pig = new net.minecraft.entity.living.mob.passive.animal.PigEntity(c.mc.world);
    var oldHit = c.mc.crosshairTarget;
    var eye = c.mc.player.getEyePosition(1);
    try {
      c.mc.crosshairTarget = new net.minecraft.world.HitResult(pig, eye.add(0, 0, 2));
      c.modules.reach.setEnabled(true);
      c.mc.interactionManager.attackEntity(c.mc.player, pig);
      check(
          "native attack records actual hit distance",
          Math.abs(c.pvp.reach.distance() - 2) < .0001);
      pig.doEvent((byte) 2);
      check("native hurt confirms combo", c.pvp.combo.combo() == 1);
      pig.doEvent((byte) 2);
      check("duplicate hurt does not confirm another hit", c.pvp.combo.combo() == 1);
      c.mc.player.doEvent((byte) 2);
      check("own hurt clears combo", c.pvp.combo.combo() == 0);
    } finally {
      c.mc.crosshairTarget = oldHit;
    }
  }

  private void checkChat() throws Exception {
    var gui = c.mc.gui.getChat();
    gui.clear();
    var field = java.util.Arrays.stream(gui.getClass().getDeclaredFields()).filter(f->java.util.List.class.isAssignableFrom(f.getType())&&f.getName().equals("messages")).findFirst().orElse(null);
    if(field==null){for(var f:gui.getClass().getDeclaredFields()){
      var type=f.getGenericType();if(type instanceof java.lang.reflect.ParameterizedType parameterized&&parameterized.getRawType()==java.util.List.class&&parameterized.getActualTypeArguments()[0]==net.minecraft.client.gui.ChatMessage.class){field=f;break;}
    }}
    if(field==null)throw new IllegalStateException("Native chat messages field not found");
    field.setAccessible(true);
    @SuppressWarnings("unchecked")
    var messages = (java.util.List<net.minecraft.client.gui.ChatMessage>) field.get(gui);
    c.modules.chat.setEnabled(true);
    c.modules.chatStack.set(true);
    c.modules.chatTimestamps.set(true);
    c.modules.qol.chatFilter.setEnabled(false);
    var original = new net.minecraft.text.LiteralText("Ornithe native chat fixture");
    original
        .getStyle()
        .setClickEvent(
            new net.minecraft.text.ClickEvent(
                net.minecraft.text.ClickEvent.Action.SUGGEST_COMMAND, "/help"));
    gui.addMessage(original);
    gui.addMessage(original);
    check(
        "native chat groups duplicates without double insertion",
        messages.size() == 1 && messages.get(0).getText().getString().contains("x2"));
    check("native timestamp added", messages.get(0).getText().getString().startsWith("["));
    check(
        "native chat preserves original component actions",
        messages.get(0).getText().getSiblings().stream()
            .anyMatch(t -> t.getStyle().getClickEvent() != null));
    c.modules.qol.chatFilter.setEnabled(true);
    c.modules.qol.chatFilterWords.set("hiddenfixture");
    gui.addMessage(new net.minecraft.text.LiteralText("hiddenfixture"));
    check("native chat filter prevents insertion", messages.size() == 1);
    c.modules.qol.chatFilter.setEnabled(false);
    c.modules.chatStack.set(false);
    c.modules.chatTimestamps.set(false);
    c.modules.qol.chatHistory.set(150);
    for (int i = 0; i < 160; i++)
      gui.addMessage(new net.minecraft.text.LiteralText("History fixture " + i));
    check("native history respects configured bound", messages.size() == 150);
    c.modules.chat.setEnabled(false);
    gui.addMessage(new net.minecraft.text.LiteralText("Vanilla limit restored"));
    check("chat disable restores Vanilla retention", messages.size() == 100);
    c.modules.chat.setEnabled(true);
    gui.clear();
  }

  private void checkCanvas() {
    var win = c.window();
    int active = org.lwjgl.opengl.GL11.glGetInteger(org.lwjgl.opengl.GL13.GL_ACTIVE_TEXTURE);
    net.minecraft.client.render.platform.GlStateManager.activeTexture(
        org.lwjgl.opengl.GL13.GL_TEXTURE1);
    long batches = c.canvas.glyphBatches;
    c.canvas.begin(win.getWidth(), win.getHeight(), win.getScale());
    try {
      c.canvas.text("Español: áéíóú ñ", 0, 0, 0xFFFFFFFF, false);
    } finally {
      c.canvas.end();
    }
    check(
        "canvas restores active texture unit",
        org.lwjgl.opengl.GL11.glGetInteger(org.lwjgl.opengl.GL13.GL_ACTIVE_TEXTURE)
            == org.lwjgl.opengl.GL13.GL_TEXTURE1);
    net.minecraft.client.render.platform.GlStateManager.activeTexture(active);
    check("Nunito batches an accented text run in one draw", c.canvas.glyphBatches - batches == 1);
    check("Unicode fallback has nonzero metrics", c.canvas.textWidth("漢字") > 0);
    int[] pixels = new int[16];
    java.util.Arrays.fill(pixels, 0xFFFF0000);
    var one = c.canvas.upload("smoke/reuse", 4, 4, pixels);
    java.util.Arrays.fill(pixels, 0xFF0000FF);
    var two = c.canvas.upload("smoke/reuse", 4, 4, pixels);
    check("dynamic texture update reuses native allocation", one.id.equals(two.id));
    c.canvas.release(two);
  }

  private void finish(Throwable error) {
    try {
      JsonObject capabilities = new JsonObject();
      for (var entry : c.capabilities.entries().entrySet()) {
        var cap = entry.getValue();
        JsonObject value = new JsonObject();
        value.addProperty("state", cap.state().name());
        value.addProperty("detail", cap.detail());
        JsonArray settings = new JsonArray();
        for (String setting : new TreeSet<>(cap.settings()))
          settings.add(new JsonPrimitive(setting));
        value.add("settings", settings);
        JsonArray hooks = new JsonArray();
        for (String hook : cap.hooks()) hooks.add(new JsonPrimitive(hook));
        value.add("hooks", hooks);
        capabilities.add(entry.getKey(), value);
      }
      if(error==null&&!restart){c.modules.coords.setEnabled(true);c.modules.crosshair.setEnabled(true);c.modules.crosshairSize.set(7);c.modules.miniPlayer.opacity.set(65);c.modules.miniPlayer.angle.set(dev.theredstonee.trsclient.core.hud.miniplayer.MiniPlayerModule.Angle.SIDE);c.modules.miniPlayer.position().offsetX=.123;c.save();c.config.flush();}
      report.add("capabilities", capabilities);report.add("videoEvidence",videoEvidence);
      report.addProperty("status", error == null ? "passed" : "failed");
      if (error != null) {
        report.addProperty("error", error.toString());
        OrnitheClient.LOG.error("Ornithe smoke failed", error);
      }
      int count = Math.min(c.frameCount, c.frameNanos.length);
      long[] frames = Arrays.copyOf(c.frameNanos, count);
      Arrays.sort(frames);
      if (count > 0) {
        report.addProperty("frames", count);
        report.addProperty("frametimeP99Ms", frames[(int) ((count - 1) * .99)] / 1e6);
        report.addProperty("frametimeMedianMs", frames[count / 2] / 1e6);
        report.addProperty("frametimeP95Ms", frames[(int) ((count - 1) * .95)] / 1e6);
        report.addProperty(
            "fpsFromMeanFrametime", 1e9 / Arrays.stream(frames).average().orElseThrow());
      }
      report.addProperty(
          "heapUsedBytes", Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory());
      Files.writeString(
          out.resolve(restart ? "restart.json" : "report.json"),
          new GsonBuilder().setPrettyPrinting().create().toJson(report));
    } catch (Exception e) {
      OrnitheClient.LOG.error("Report failed", e);
    } finally {
      stage = 99;
      c.mc.stop();
    }
  }
}
