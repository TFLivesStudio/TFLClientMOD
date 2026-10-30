package dev.theredstonee.trsclient.ornithe.dev;

import dev.theredstonee.trsclient.ornithe.OrnitheClient;
import java.awt.image.BufferedImage;
import java.nio.ByteBuffer;
import java.nio.file.Path;
import java.util.*;
import javax.imageio.ImageIO;
import net.minecraft.client.render.platform.GlStateManager;
import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.*;

/** Real native font pixels, including cached font binds after minimap/fog rendering. Probe only. */
public final class MapFontRegression {
  private static final int WIDTH = 256, HEIGHT = 48;

  private static AutoCloseable scope() throws Exception {
    Class<?> type = Class.forName("dev.theredstonee.trsclient.ornithe.MapGlState");
    var constructor = type.getDeclaredConstructor();
    constructor.setAccessible(true);
    Object value = constructor.newInstance();
    var begin = type.getDeclaredMethod("begin");
    begin.setAccessible(true);
    begin.invoke(value);
    return (AutoCloseable) value;
  }

  private static byte[] raster(OrnitheClient c, Path out, String name) throws Exception {
    GL11.glDisable(GL11.GL_SCISSOR_TEST);
    GL11.glClearColor(0, 0, 0, 1);
    GL11.glClear(GL11.GL_COLOR_BUFFER_BIT);
    c.mc.textRenderer.draw("Minecraft 123 ABC xyz / ñá", 4, 8, 0xffffffff, false);
    ByteBuffer pixels = BufferUtils.createByteBuffer(WIDTH * HEIGHT * 4);
    GL11.glReadPixels(0, 0, WIDTH, HEIGHT, GL11.GL_RGBA, GL11.GL_UNSIGNED_BYTE, pixels);
    byte[] data = new byte[pixels.capacity()];
    pixels.get(data);
    int lit = 0;
    BufferedImage image = new BufferedImage(WIDTH, HEIGHT, BufferedImage.TYPE_INT_ARGB);
    for (int y = 0; y < HEIGHT; y++)
      for (int x = 0; x < WIDTH; x++) {
        int p = (y * WIDTH + x) * 4,
            r = data[p] & 255,
            g = data[p + 1] & 255,
            b = data[p + 2] & 255;
        if (r + g + b > 200) lit++;
        image.setRGB(x, HEIGHT - y - 1, 0xff000000 | r << 16 | g << 8 | b);
      }
    if (lit < 100) throw new AssertionError("Native font baseline contains no readable glyphs");
    java.nio.file.Files.createDirectories(out);
    ImageIO.write(image, "png", out.resolve(name + ".png").toFile());
    return data;
  }

  public static Map<String, Object> target(OrnitheClient c, Path out) throws Exception {
    var field = OrnitheClient.class.getDeclaredField("minimapTarget");
    field.setAccessible(true);
    Object target = field.get(c);
    var tex = target.getClass().getDeclaredField("texture");
    tex.setAccessible(true);
    var edgeField = target.getClass().getDeclaredField("edge");
    edgeField.setAccessible(true);
    int edge = edgeField.getInt(target);
    try (AutoCloseable ignored = scope()) {
      GlStateManager.bindTexture(tex.getInt(target));
      ByteBuffer pixels = BufferUtils.createByteBuffer(edge * edge * 4);
      GL11.glGetTexImage(GL11.GL_TEXTURE_2D, 0, GL11.GL_RGBA, GL11.GL_UNSIGNED_BYTE, pixels);
      int opaque = 0;
      BufferedImage image = new BufferedImage(edge, edge, BufferedImage.TYPE_INT_ARGB);
      for (int y = 0; y < edge; y++)
        for (int x = 0; x < edge; x++) {
          int p = (y * edge + x) * 4,
              r = pixels.get(p) & 255,
              g = pixels.get(p + 1) & 255,
              b = pixels.get(p + 2) & 255,
              a = pixels.get(p + 3) & 255;
          if (a > 200) opaque++;
          image.setRGB(x, edge - y - 1, a << 24 | r << 16 | g << 8 | b);
        }
      ImageIO.write(image, "png", out.resolve("minimap-target-pause.png").toFile());
      return Map.of("edge", edge, "opaquePixels", opaque, "totalPixels", edge * edge);
    }
  }

  public static Map<String, Object> run(OrnitheClient c, Path out) throws Exception {
    if (!GLContext.getCapabilities().OpenGL30)
      throw new AssertionError("Native map font fixture requires framebuffer support");
    int fbo = 0, texture = 0;
    boolean enabled = c.modules.minimap.isEnabled();
    try (AutoCloseable ignored = scope()) {
      fbo = GL30.glGenFramebuffers();
      texture = GL11.glGenTextures();
      GlStateManager.bindTexture(texture);
      GL11.glTexImage2D(
          GL11.GL_TEXTURE_2D,
          0,
          GL11.GL_RGBA8,
          WIDTH,
          HEIGHT,
          0,
          GL11.GL_RGBA,
          GL11.GL_UNSIGNED_BYTE,
          (ByteBuffer) null);
      GL30.glBindFramebuffer(GL30.GL_FRAMEBUFFER, fbo);
      GL30.glFramebufferTexture2D(
          GL30.GL_FRAMEBUFFER, GL30.GL_COLOR_ATTACHMENT0, GL11.GL_TEXTURE_2D, texture, 0);
      if (GL30.glCheckFramebufferStatus(GL30.GL_FRAMEBUFFER) != GL30.GL_FRAMEBUFFER_COMPLETE)
        throw new AssertionError("Font fixture framebuffer incomplete");
      GL11.glViewport(0, 0, WIDTH, HEIGHT);
      GL11.glMatrixMode(GL11.GL_PROJECTION);
      GL11.glLoadIdentity();
      GL11.glOrtho(0, WIDTH, HEIGHT, 0, -1000, 1000);
      GL11.glMatrixMode(GL11.GL_MODELVIEW);
      GL11.glLoadIdentity();
      byte[] before = raster(c, out, "font-before-map");
      for (int i = 0; i < 3; i++) {
        c.modules.minimap.setEnabled(i != 0);
        GlStateManager.activeTexture(GL13.GL_TEXTURE1);
        GlStateManager.enableTexture();
        int binding = GL11.glGetInteger(GL11.GL_TEXTURE_BINDING_2D);
        boolean texturing = GL11.glIsEnabled(GL11.GL_TEXTURE_2D);
        c.drawHud();
        if (GL11.glGetInteger(GL13.GL_ACTIVE_TEXTURE) != GL13.GL_TEXTURE1
            || GL11.glGetInteger(GL11.GL_TEXTURE_BINDING_2D) != binding
            || GL11.glIsEnabled(GL11.GL_TEXTURE_2D) != texturing)
          throw new AssertionError("Minimap leaked active texture/lightmap state");
        GlStateManager.disableTexture();
        GlStateManager.activeTexture(GL13.GL_TEXTURE0);
        byte[] after = raster(c, out, "font-after-minimap-" + i);
        if (!Arrays.equals(before, after))
          throw new AssertionError("Native glyph pixels changed after minimap toggle " + i);
      }
      c.canvas.begin(c.window().getWidth(), c.window().getHeight(), c.window().getScale());
      try {
        c.diorama.draw(
            c.canvas,
            0,
            0,
            WIDTH,
            HEIGHT,
            c.mc.player.x + 512,
            c.mc.player.y,
            c.mc.player.z + 512,
            .5);
      } finally {
        c.canvas.end();
      }
      if (!Arrays.equals(before, raster(c, out, "font-after-world-map")))
        throw new AssertionError("Native glyph pixels changed after full-map fog");
      return Map.of(
          "nativeGlyphPixelsIdentical", true, "minimapToggleDraws", 3, "worldMapDraws", 1);
    } finally {
      c.modules.minimap.setEnabled(enabled);
      if (fbo != 0) GL30.glDeleteFramebuffers(fbo);
      if (texture != 0) GL11.glDeleteTextures(texture);
    }
  }
}
