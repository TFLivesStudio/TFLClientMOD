package dev.theredstonee.trsclient.ornithe;

import dev.theredstonee.trsclient.core.ui.*;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.InputStream;
import java.util.UUID;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiElement;
import net.minecraft.client.render.platform.GlStateManager;
import net.minecraft.client.render.texture.DynamicTexture;
import net.minecraft.resource.Identifier;
import org.lwjgl.opengl.GL11;

/**
 * Feather/Ornithe bridge for the existing common canvas. Assets are prepared at READY/reload, never
 * in draw.
 */
public final class OrnitheCanvas implements Canvas, Textures.Store {
  private final Minecraft mc;
  private Identifier font;
  private final int[] advances = new int[384];
  private static final int CELL = 52, COLS = 24, FIRST = 32, LAST = 384;
  private int width, height, scale = 1;
  private final Identifier still = new Identifier("trsclient:textures/menu/black-hole.png");
  private final Identifier icons = new Identifier("trsclient:textures/ui/icons.png");

  public OrnitheCanvas(Minecraft mc) {
    this.mc = mc;
    Textures.install(this);
  }

  public void prepare() {
    if (font != null) {
      mc.getTextureManager().close(font);
      font = null;
    }
    try (InputStream stream =
        mc.getResourceManager()
            .getResource(new Identifier("trsclient:font/nunito-regular.ttf"))
            .asStream()) {
      Font face = Font.createFont(Font.TRUETYPE_FONT, stream).deriveFont(36f);
      BufferedImage image =
          new BufferedImage(
              COLS * CELL, ((LAST - FIRST + COLS - 1) / COLS) * CELL, BufferedImage.TYPE_INT_ARGB);
      Graphics2D g = image.createGraphics();
      try {
        g.setFont(face);
        g.setColor(java.awt.Color.WHITE);
        g.setRenderingHint(
            RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        g.setRenderingHint(
            RenderingHints.KEY_FRACTIONALMETRICS, RenderingHints.VALUE_FRACTIONALMETRICS_ON);
        for (int ch = FIRST; ch < LAST; ch++) {
          int n = ch - FIRST;
          advances[ch] =
              (int)
                  Math.ceil(
                      face.getStringBounds(String.valueOf((char) ch), g.getFontRenderContext())
                          .getWidth());
          g.drawString(
              String.valueOf((char) ch),
              n % COLS * CELL + 2,
              n / COLS * CELL + 2 + g.getFontMetrics().getAscent());
        }
      } finally {
        g.dispose();
      }
      font = mc.getTextureManager().register("tflcore/nunito", new DynamicTexture(image));
      mc.getTextureManager().bind(font);
      GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MIN_FILTER, GL11.GL_LINEAR);
      GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MAG_FILTER, GL11.GL_LINEAR);
    } catch (Exception e) {
      OrnitheClient.LOG.warn("Nunito unavailable; retaining Minecraft font", e);
    }
    mc.getTextureManager().bind(still);
    mc.getTextureManager().bind(icons);
    GlStateManager.color4f(1, 1, 1, 1);
  }

  public void begin(int width, int height, int scale) {
    this.width = width;
    this.height = height;
    this.scale = scale;
    GlStateManager.disableLighting();
    GlStateManager.enableTexture();
    GlStateManager.enableBlend();
    GlStateManager.blendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
  }

  public void fill(int x1, int y1, int x2, int y2, int color) {
    GuiElement.fill(x1, y1, x2, y2, color);
  }

  public void text(String text, int x, int y, int color, boolean shadow) {
    if (font == null) {
      mc.textRenderer.draw(text, x, y, color, shadow);
      return;
    }
    if (shadow) text(text, x + 1, y + 1, (color & 0xFF000000) | ((color & 0xFCFCFC) >> 2), false);
    mc.getTextureManager().bind(font);
    GlStateManager.enableBlend();
    GlStateManager.color4f(
        (color >> 16 & 255) / 255f,
        (color >> 8 & 255) / 255f,
        (color & 255) / 255f,
        (color >>> 24) / 255f);
    float cursor = x;
    for (int i = 0; i < text.length(); i++) {
      char ch = text.charAt(i);
      if (ch == '\u00a7' && i + 1 < text.length()) {
        i++;
        continue;
      }
      if (ch < FIRST || ch >= LAST) ch = '?';
      int n = ch - FIRST;
      push();
      translate(cursor - .5f, y - .5f);
      scale(.25f);
      GuiElement.drawTexture(
          0,
          0,
          n % COLS * CELL,
          n / COLS * CELL,
          CELL,
          CELL,
          COLS * CELL,
          ((LAST - FIRST + COLS - 1) / COLS) * CELL);
      pop();
      cursor += advances[ch] / 4f;
    }
    GlStateManager.color4f(1, 1, 1, 1);
  }

  public int textWidth(String text) {
    if (font == null) return mc.textRenderer.getWidth(text);
    float w = 0;
    for (int i = 0; i < text.length(); i++) {
      char ch = text.charAt(i);
      if (ch == '\u00a7' && i + 1 < text.length()) {
        i++;
        continue;
      }
      w += advances[ch >= FIRST && ch < LAST ? ch : '?'] / 4f;
    }
    return (int) Math.ceil(w);
  }

  public int lineHeight() {
    return 10;
  }

  public String clip(String text, int max) {
    if (textWidth(text) <= max) return text;
    int end = text.length();
    while (end > 0 && textWidth(text.substring(0, end)) > Math.max(0, max)) end--;
    return text.substring(0, end);
  }

  public void flush() {}

  public void scissor(int x1, int y1, int x2, int y2) {
    GL11.glEnable(GL11.GL_SCISSOR_TEST);
    GL11.glScissor(
        x1 * scale,
        mc.height - y2 * scale,
        Math.max(0, (x2 - x1) * scale),
        Math.max(0, (y2 - y1) * scale));
  }

  public void noScissor() {
    GL11.glDisable(GL11.GL_SCISSOR_TEST);
  }

  public void raise(float z) {
    GlStateManager.translatef(0, 0, z);
  }

  public void push() {
    GlStateManager.pushMatrix();
  }

  public void pop() {
    GlStateManager.popMatrix();
  }

  public void translate(float x, float y) {
    GlStateManager.translatef(x, y, 0);
  }

  public void scale(float f) {
    GlStateManager.scalef(f, f, 1);
  }

  public void scale(float x, float y) {
    GlStateManager.scalef(x, y, 1);
  }

  public void rotate(float radians) {
    GlStateManager.rotatef((float) Math.toDegrees(radians), 0, 0, 1);
  }

  public boolean images() {
    return true;
  }

  public void image(TextureRef tex, float u, float v, int w, int h, int color) {
    mc.getTextureManager().bind((Identifier) tex.id);
    GlStateManager.enableBlend();
    GlStateManager.color4f(
        (color >> 16 & 255) / 255f,
        (color >> 8 & 255) / 255f,
        (color & 255) / 255f,
        (color >>> 24) / 255f);
    GuiElement.drawTexture(0, 0, u, v, w, h, tex.width, tex.height);
    GlStateManager.color4f(1, 1, 1, 1);
  }

  public TextureRef game(String location, int w, int h) {
    return new TextureRef(new Identifier(location), w, h);
  }

  public TextureRef upload(String name, int w, int h, int[] pixels) {
    DynamicTexture tex = new DynamicTexture(w, h);
    System.arraycopy(pixels, 0, tex.getPixels(), 0, w * h);
    tex.upload();
    return new TextureRef(mc.getTextureManager().register("tflcore/" + name, tex), w, h);
  }

  public void release(TextureRef texture) {
    mc.getTextureManager().close((Identifier) texture.id);
  }

  public Textures.DefaultSkin defaultSkin(UUID id) {
    return new Textures.DefaultSkin(game("minecraft:textures/entity/steve.png", 64, 64), false);
  }
}
