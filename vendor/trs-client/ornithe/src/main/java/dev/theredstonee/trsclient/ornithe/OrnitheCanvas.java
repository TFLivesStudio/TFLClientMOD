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
  private int scope,
      oldTexture,
      oldSrcRgb,
      oldDstRgb,
      oldSrcAlpha,
      oldDstAlpha,
      oldMatrixMode,
      oldMatrixDepth,
      oldActiveTexture;
  private boolean oldBlend,
      oldLighting,
      oldTexturing,
      oldDepth,
      oldAlpha,
      oldCull,
      oldScissor,
      oldRescale,
      oldNormalize,
      oldDepthMask,
      oldLight0,
      oldLight1,
      oldColorMaterial;
  private final java.nio.IntBuffer savedScissor = org.lwjgl.BufferUtils.createIntBuffer(16);
  private final java.nio.FloatBuffer savedColor = org.lwjgl.BufferUtils.createFloatBuffer(16);
  public long glyphBatches, glyphsRendered;

  private record DynamicEntry(DynamicTexture texture, TextureRef ref) {}

  private final java.util.Map<String, DynamicEntry> dynamic = new java.util.HashMap<>();

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
    if (scope++ == 0) {
      oldActiveTexture = GL11.glGetInteger(org.lwjgl.opengl.GL13.GL_ACTIVE_TEXTURE);
      GlStateManager.activeTexture(org.lwjgl.opengl.GL13.GL_TEXTURE0);
      oldLight0 = GL11.glIsEnabled(GL11.GL_LIGHT0);
      oldLight1 = GL11.glIsEnabled(GL11.GL_LIGHT1);
      oldColorMaterial = GL11.glIsEnabled(GL11.GL_COLOR_MATERIAL);
      oldBlend = GL11.glIsEnabled(GL11.GL_BLEND);
      oldLighting = GL11.glIsEnabled(GL11.GL_LIGHTING);
      oldTexturing = GL11.glIsEnabled(GL11.GL_TEXTURE_2D);
      oldDepth = GL11.glIsEnabled(GL11.GL_DEPTH_TEST);
      oldAlpha = GL11.glIsEnabled(GL11.GL_ALPHA_TEST);
      oldCull = GL11.glIsEnabled(GL11.GL_CULL_FACE);
      oldScissor = GL11.glIsEnabled(GL11.GL_SCISSOR_TEST);
      oldRescale = GL11.glIsEnabled(0x803A);
      oldNormalize = GL11.glIsEnabled(GL11.GL_NORMALIZE);
      oldDepthMask = GL11.glGetBoolean(GL11.GL_DEPTH_WRITEMASK);
      oldTexture = GL11.glGetInteger(GL11.GL_TEXTURE_BINDING_2D);
      oldSrcRgb = GL11.glGetInteger(org.lwjgl.opengl.GL14.GL_BLEND_SRC_RGB);
      oldDstRgb = GL11.glGetInteger(org.lwjgl.opengl.GL14.GL_BLEND_DST_RGB);
      oldSrcAlpha = GL11.glGetInteger(org.lwjgl.opengl.GL14.GL_BLEND_SRC_ALPHA);
      oldDstAlpha = GL11.glGetInteger(org.lwjgl.opengl.GL14.GL_BLEND_DST_ALPHA);
      savedColor.clear();
      GL11.glGetFloat(GL11.GL_CURRENT_COLOR, savedColor);
      savedScissor.clear();
      GL11.glGetInteger(GL11.GL_SCISSOR_BOX, savedScissor);
      oldMatrixMode = GL11.glGetInteger(GL11.GL_MATRIX_MODE);
      GlStateManager.matrixMode(GL11.GL_MODELVIEW);
      oldMatrixDepth = GL11.glGetInteger(GL11.GL_MODELVIEW_STACK_DEPTH);
      GlStateManager.pushMatrix();
    }
    GlStateManager.disableLighting();
    GlStateManager.enableTexture();
    GlStateManager.enableBlend();
    GlStateManager.blendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
  }

  public void end() {
    if (scope <= 0) throw new IllegalStateException("Unbalanced canvas scope");
    if (--scope != 0) return;
    GlStateManager.activeTexture(org.lwjgl.opengl.GL13.GL_TEXTURE0);
    GlStateManager.matrixMode(GL11.GL_MODELVIEW);
    while (GL11.glGetInteger(GL11.GL_MODELVIEW_STACK_DEPTH) > oldMatrixDepth)
      GlStateManager.popMatrix();
    GlStateManager.matrixMode(oldMatrixMode);
    if (oldLight0) GlStateManager.enableLight(0);
    else GlStateManager.disableLight(0);
    if (oldLight1) GlStateManager.enableLight(1);
    else GlStateManager.disableLight(1);
    if (oldColorMaterial) GlStateManager.enableColorMaterial();
    else GlStateManager.disableColorMaterial();
    if (oldBlend) GlStateManager.enableBlend();
    else GlStateManager.disableBlend();
    if (oldLighting) GlStateManager.enableLighting();
    else GlStateManager.disableLighting();
    if (oldTexturing) GlStateManager.enableTexture();
    else GlStateManager.disableTexture();
    if (oldDepth) GlStateManager.enableDepthTest();
    else GlStateManager.disableDepthTest();
    if (oldAlpha) GlStateManager.enableAlphaTest();
    else GlStateManager.disableAlphaTest();
    if (oldCull) GlStateManager.enableCull();
    else GlStateManager.disableCull();
    if (oldRescale) GlStateManager.enableRescaleNormal();
    else GlStateManager.disableRescaleNormal();
    if (oldNormalize) GlStateManager.enableNormalize();
    else GlStateManager.disableNormalize();
    GlStateManager.depthMask(oldDepthMask);
    GlStateManager.blendFuncSeparate(oldSrcRgb, oldDstRgb, oldSrcAlpha, oldDstAlpha);
    GlStateManager.bindTexture(oldTexture);
    GlStateManager.color4f(
        savedColor.get(0), savedColor.get(1), savedColor.get(2), savedColor.get(3));
    if (oldScissor) {
      GL11.glEnable(GL11.GL_SCISSOR_TEST);
      GL11.glScissor(
          savedScissor.get(0), savedScissor.get(1), savedScissor.get(2), savedScissor.get(3));
    } else GL11.glDisable(GL11.GL_SCISSOR_TEST);
    GlStateManager.activeTexture(oldActiveTexture);
  }

  public void fill(int x1, int y1, int x2, int y2, int color) {
    GuiElement.fill(x1, y1, x2, y2, color);
  }

  private boolean nativeFont(String text) {
    for (int i = 0; i < text.length(); i++) {
      char ch = text.charAt(i);
      if (ch == '\u00a7' || ch < FIRST || ch >= LAST) return true;
    }
    return font == null;
  }

  private String privateText(String text) {
    var c = OrnitheClient.get();
    return c != null && c.ready ? c.privacy.text(text) : text;
  }

  public void text(String text, int x, int y, int color, boolean shadow) {
    text = privateText(text);
    if (text.isEmpty()) return;
    if (nativeFont(text)) {
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
    var tess = net.minecraft.client.render.vertex.Tesselator.getInstance();
    var buffer = tess.getBuffer();
    buffer.begin(
        GL11.GL_QUADS, net.minecraft.client.render.vertex.DefaultVertexFormat.POSITION_TEX);
    double cursor = x - .5,
        top = y - .5,
        size = CELL * .25,
        atlasW = COLS * CELL,
        atlasH = ((LAST - FIRST + COLS - 1) / COLS) * CELL;
    for (int i = 0; i < text.length(); i++) {
      char ch = text.charAt(i);
      int n = ch - FIRST;
      double u = (n % COLS * CELL) / atlasW,
          v = (n / COLS * CELL) / atlasH,
          du = CELL / atlasW,
          dv = CELL / atlasH;
      buffer.vertex(cursor, top + size, 0).texture(u, v + dv).nextVertex();
      buffer.vertex(cursor + size, top + size, 0).texture(u + du, v + dv).nextVertex();
      buffer.vertex(cursor + size, top, 0).texture(u + du, v).nextVertex();
      buffer.vertex(cursor, top, 0).texture(u, v).nextVertex();
      cursor += advances[ch] / 4.0;
    }
    tess.end();
    glyphBatches++;
    glyphsRendered += text.length();
    GlStateManager.color4f(1, 1, 1, 1);
  }

  public int textWidth(String text) {
    text = privateText(text);
    if (nativeFont(text)) return mc.textRenderer.getWidth(text);
    double width = 0;
    for (int i = 0; i < text.length(); i++) width += advances[text.charAt(i)] / 4.0;
    return (int) Math.ceil(width);
  }

  public int lineHeight() {
    return 10;
  }

  public String clip(String text, int max) {
    text = privateText(text);
    if (max <= 0) return "";
    if (nativeFont(text)) return mc.textRenderer.trim(text, max);
    double width = 0;
    int end = 0;
    while (end < text.length()) {
      double next = advances[text.charAt(end)] / 4.0;
      if (width + next > max) break;
      width += next;
      end++;
    }
    return end == text.length() ? text : text.substring(0, end);
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
    if (!Textures.validName(name) || w <= 0 || h <= 0 || pixels.length < w * h)
      throw new IllegalArgumentException("Texture dimensions/name");
    DynamicEntry entry = dynamic.get(name);
    if (entry == null || entry.ref.width != w || entry.ref.height != h) {
      if (entry != null) release(entry.ref);
      DynamicTexture texture = new DynamicTexture(w, h);
      TextureRef ref =
          new TextureRef(mc.getTextureManager().register("tflcore/" + name, texture), w, h);
      entry = new DynamicEntry(texture, ref);
      dynamic.put(name, entry);
    }
    System.arraycopy(pixels, 0, entry.texture.getPixels(), 0, w * h);
    entry.texture.upload();
    return entry.ref;
  }

  public int ownedDynamicTextures(){return dynamic.size()+(font==null?0:1);}
  public long ownedDynamicTextureBytes(){long bytes=font==null?0:(long)COLS*CELL*((LAST-FIRST+COLS-1)/COLS)*CELL*4;for(var entry:dynamic.values())bytes+=(long)entry.ref.width*entry.ref.height*4;return bytes;}

  public void release(TextureRef texture) {
    dynamic.values().removeIf(e -> e.ref.id.equals(texture.id));
    mc.getTextureManager().close((Identifier) texture.id);
  }

  public Textures.DefaultSkin defaultSkin(UUID id) {
    return new Textures.DefaultSkin(game("minecraft:textures/entity/steve.png", 64, 64), false);
  }
}
