package dev.theredstonee.trsclient.ornithe;

import dev.theredstonee.trsclient.core.render.ColorGrade;
import dev.theredstonee.trsclient.ornithe.capability.CapabilityRegistry.State;
import net.minecraft.client.render.*;
import net.minecraft.client.render.platform.GlStateManager;
import net.minecraft.resource.Identifier;
import org.lwjgl.opengl.GL11;

/** Shared affine grading through Vanilla's own post-process API, before HUD/UI. */
public final class OrnitheColors {
  private final OrnitheClient c;
  private final ColorGrade grade = new ColorGrade();
  private final float[] matrix = new float[12];
  private final float[] previous = {Float.NaN, 0, 0, 0, 0};
  private PostChain chain;
  private PostPass pass;
  private int width = -1, height = -1;
  private boolean failed;
  public long applied;

  public OrnitheColors(OrnitheClient c) {
    this.c = c;
    c.capabilities.connect(
        c.modules.colors,
        State.EXPERIMENTAL,
        "Shared color matrix/vibrance with Vanilla post-processing. Skips existing Vanilla shaders;"
            + " no second world renderer. GPU hardware performance remains unmeasured.",
        c.modules.colors.settings().stream().map(s -> s.key()).toList(),
        "OrnitheColors",
        "ColorGrade",
        "GameRendererMixin.renderWorld",
        "PostChain");
  }

  public void apply(float partial) {
    if (failed) return;
    if (!c.capabilities.active(c.modules.colors) || c.mc.world == null) {
      release();
      return;
    }
    c.modules.colorGrade(grade);
    if (grade.identity() || c.mc.gameRenderer.hasShader() || !c.mc.options.fboEnable) return;
    try {
      var main = c.mc.getRenderTarget();
      if (chain == null) {
        chain =
            new PostChain(
                c.mc.getTextureManager(),
                c.mc.getResourceManager(),
                main,
                new Identifier("trsclient:shaders/post/color.json"));
        var swap = chain.getTempTarget("swap");
        pass = chain.addPass("trsclient_color", main, swap);
        chain.addPass("blit", swap, main);
        width = height = -1;
      }
      if (width != c.mc.width || height != c.mc.height) {
        width = c.mc.width;
        height = c.mc.height;
        chain.resize(width, height);
      }
      if (previous[0] != grade.saturation
          || previous[1] != grade.contrast
          || previous[2] != grade.brightness
          || previous[3] != grade.vibrance
          || previous[4] != grade.temperature) {
        grade.matrix(matrix);
        previous[0] = grade.saturation;
        previous[1] = grade.contrast;
        previous[2] = grade.brightness;
        previous[3] = grade.vibrance;
        previous[4] = grade.temperature;
      }
      var effect = pass.getEffect();
      for (int i = 0; i < 3; i++) {
        var uniform = effect.getUniform(i == 0 ? "RowR" : i == 1 ? "RowG" : "RowB");
        if (uniform != null)
          uniform.set(matrix[i * 4], matrix[i * 4 + 1], matrix[i * 4 + 2], matrix[i * 4 + 3]);
      }
      var vibrance = effect.getUniform("Vibrance");
      if (vibrance != null) vibrance.set(grade.vibrance);
      int mode = GL11.glGetInteger(GL11.GL_MATRIX_MODE);
      GlStateManager.matrixMode(GL11.GL_TEXTURE);
      GlStateManager.pushMatrix();
      GlStateManager.loadIdentity();
      try {
        chain.process(partial);
        applied++;
      } finally {
        GlStateManager.matrixMode(GL11.GL_TEXTURE);
        GlStateManager.popMatrix();
        GlStateManager.matrixMode(mode);
        main.bindWrite(true);
      }
    } catch (Exception e) {
      failed = true;
      release();
      OrnitheClient.LOG.warn("Native color grading unavailable", e);
      c.capabilities.declare(
          "colors",
          State.CONFLICT,
          "Native shader initialization failed; original rendering retained. Check the client"
              + " log.");
    }
  }

  public boolean allocated() {
    return chain != null;
  }

  public void release() {
    if (chain != null) chain.close();
    chain = null;
    pass = null;
    width = height = -1;
  }
}
