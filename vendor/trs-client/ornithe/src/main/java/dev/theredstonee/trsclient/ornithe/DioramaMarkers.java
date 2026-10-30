package dev.theredstonee.trsclient.ornithe;

import dev.diorama.Projection;
import dev.diorama.render.*;
import dev.theredstonee.trsclient.core.ui.*;
import dev.theredstonee.trsclient.core.waypoint.Waypoint;
import java.util.*;
import java.util.concurrent.*;

/** Uses Diorama's actual voxel arrow and pin art; generation never waits on the render thread. */
final class DioramaMarkers implements AutoCloseable {
  private final OrnitheClient client;
  private final Map<String, TextureRef> textures = new LinkedHashMap<>(80, .75f, true);
  private final Map<String, CompletableFuture<PinArt.Image>> arrows = new HashMap<>();
  private final ExecutorService worker =
      Executors.newSingleThreadExecutor(
          r -> {
            Thread t = new Thread(r, "TFL-Diorama-markers");
            t.setDaemon(true);
            return t;
          });
  private final Map<String, PinArt.Art> pins = new HashMap<>();
  private final Map<String, Optional<PinArt.Image>> icons = new HashMap<>();

  DioramaMarkers(OrnitheClient client) {
    this.client = client;
  }

  private TextureRef upload(String key, PinArt.Image image) {
    int[] pixels = new int[image.width * image.height];
    for (int y = 0; y < image.height; y++)
      for (int x = 0; x < image.width; x++) pixels[y * image.width + x] = image.argb(x, y);
    TextureRef texture =
        client.canvas.upload(
            "diorama/marker-" + Integer.toHexString(key.hashCode()),
            image.width,
            image.height,
            pixels);
    textures.put(key, texture);
    while (textures.size() > 96) {
      var it = textures.entrySet().iterator();
      var old = it.next();
      it.remove();
      client.canvas.release(old.getValue());
    }
    return texture;
  }

  void player(Canvas c, double x, double y, float yaw, int q) {
    double heading = yaw + q * 90.0;
    int h = Math.floorMod((int) Math.round(heading / 5), 72);
    int accent = Theme.get().accent & 0xffffff;
    String key = "arrow-" + h + "-" + accent;
    int uploaded = 0;
    for (var it = arrows.entrySet().iterator(); it.hasNext() && uploaded < 2; ) {
      var pending = it.next();
      if (pending.getValue().isDone()) {
        upload(pending.getKey(), pending.getValue().join());
        it.remove();
        uploaded++;
      }
    }
    TextureRef texture = textures.get(key);
    if (texture == null && arrows.size() < 8) {
      double sin = Projection.sin(), cos = Projection.cos();
      var future =
          arrows.computeIfAbsent(
              key,
              k ->
                  CompletableFuture.supplyAsync(
                      () -> ArrowArt.voxel(h * 5, 6, sin, cos, accent), worker));
      if (future.isDone()) {
        texture = upload(key, future.join());
        arrows.remove(key);
      }
    }
    if (texture != null)
      Affine.image(
          c,
          texture,
          (float) x - 18,
          (float) y - 18,
          36,
          36,
          0,
          0,
          texture.width,
          texture.height,
          0xffffffff);
    else {
      c.push();
      c.translate((float) x, (float) y);
      double dx = -Math.sin(Math.toRadians(heading)),
          dy = Math.cos(Math.toRadians(heading)) * Projection.sin();
      c.rotate((float) Math.toDegrees(Math.atan2(-dx, dy)));
      for (int row = 0; row < 8; row++) c.fill(-row / 2, row - 5, row / 2 + 1, row - 4, 0xffffffff);
      c.pop();
    }
  }

  private PinArt.Image icon(String id) {
    if (id == null || !id.matches("[a-z0-9_.-]+:[a-z0-9_./-]+") || id.contains("..")) return null;
    return icons
        .computeIfAbsent(
            id,
            key -> {
              try {
                String[] parts = key.split(":", 2);
                byte[] bytes =
                    client.mapPlatform.readResource(
                        parts[0], "textures/waypoint_icons/" + parts[1] + ".png");
                var image = javax.imageio.ImageIO.read(new java.io.ByteArrayInputStream(bytes));
                if (image == null || image.getWidth() > 128 || image.getHeight() > 128)
                  return Optional.empty();
                return Optional.of(
                    PinArt.fromArgb(
                        image.getWidth(),
                        image.getHeight(),
                        image.getRGB(
                            0, 0, image.getWidth(), image.getHeight(), null, 0, image.getWidth())));
              } catch (java.io.IOException e) {
                return Optional.empty();
              }
            })
        .orElse(null);
  }

  void waypoint(Canvas c, Waypoint point, int x, int y, boolean label) {
    String key = "pin-" + point.color + "-" + point.death + "-" + point.icon;
    PinArt.Art art =
        pins.computeIfAbsent(
            key,
            k -> {
              var image = icon(point.icon);
              return image == null
                  ? PinArt.standing(point.color, point.death, 13)
                  : PinArt.iconPin(image, point.color);
            });
    TextureRef texture = textures.get(key);
    if (texture == null) texture = upload(key, art.image());
    float left = x - art.anchorX(), top = y - art.anchorY();
    Affine.image(
        c,
        texture,
        left,
        top,
        art.image().width,
        art.image().height,
        0,
        0,
        art.image().width,
        art.image().height,
        0xffffffff);
    if (point.icon == null || point.icon.isEmpty()) {
      String initials =
          point.initials == null || point.initials.isEmpty()
              ? initials(point.name)
              : point.initials;
      String text = c.clip(initials, 10);
      c.text(
          text,
          (int) (left + (art.labelX0() + art.labelX1() - c.textWidth(text)) / 2),
          (int) (top + (art.labelY0() + art.labelY1() - c.lineHeight()) / 2),
          PinArt.light(point.color) ? 0xff101418 : 0xffffffff,
          false);
    }
    if (label) c.text(point.name, x + 12, y - 15, Theme.get().text, true);
  }

  static String initials(String name) {
    if (name == null || name.isEmpty()) return "?";
    String[] words = name.trim().split("\\s+");
    return words.length > 1
        ? (words[0].substring(0, 1) + words[words.length - 1].substring(0, 1))
            .toUpperCase(java.util.Locale.ROOT)
        : name.substring(0, Math.min(2, name.length())).toUpperCase(java.util.Locale.ROOT);
  }

  void reload() {
    for (TextureRef t : textures.values()) client.canvas.release(t);
    textures.clear();
    arrows.clear();
    icons.clear();
    pins.clear();
  }

  public void close() {
    reload();
    worker.shutdownNow();
  }
}
