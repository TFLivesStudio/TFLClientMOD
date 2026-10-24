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

  private float fov() throws Exception {
    var method =
        c.mc.gameRenderer.getClass().getDeclaredMethod("getFov", float.class, boolean.class);
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

  @Override
  public void run() {
    try {
      tick++;
      if (System.nanoTime() - started > 180_000_000_000L)
        throw new AssertionError("client timeout");
      if (--wait > 0) return;
      switch (stage) {
        case 0 -> {
          c.mc.options.pauseOnUnfocus = false;
          check(
              "shared main menu", c.mc.screen instanceof CoreScreen cs && cs.ui instanceof TitleUi);
          if (restart) {
            check(
                "preferences loaded after process restart",
                c.modules.coords.isEnabled()
                    && c.modules.crosshair.isEnabled()
                    && c.modules.crosshairSize.get() == 7);
            shot("restart-title");
            finish(null);
            return;
          }
          wait = 25;
          stage++;
        }
        case 1 -> {
          shot("title");
          KeyBinding.click(c.menuKey.getKeyCode());
          wait = 15;
          stage++;
        }
        case 2 -> {
          check(
              "registered menu key opens common settings",
              c.mc.screen instanceof CoreScreen cs && cs.ui instanceof PocMenu);
          shot("settings");
          c.mc.openScreen(null);
          check(
              "null-screen return uses shared title",
              c.mc.screen instanceof CoreScreen cs && cs.ui instanceof TitleUi);
          new PocHost(c, null).openHudEditor();
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
              "tfl-ornithe-smoke",
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
            if (OrnitheClient.SUPPORTED.contains(m.id())) {
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
          finish(null);
        }
      }
    } catch (Throwable e) {
      finish(e);
    }
  }

  private void finish(Throwable error) {
    try {
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
