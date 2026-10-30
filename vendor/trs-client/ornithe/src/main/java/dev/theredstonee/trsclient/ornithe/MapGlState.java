package dev.theredstonee.trsclient.ornithe;

import java.nio.*;
import net.minecraft.client.render.platform.GlStateManager;
import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.*;

/** Map-only scope. Never use glPopAttrib: it silently invalidates Minecraft's cached state. */
final class MapGlState implements AutoCloseable {
  private final IntBuffer viewport = BufferUtils.createIntBuffer(16),
      scissor = BufferUtils.createIntBuffer(16);
  private final FloatBuffer color = BufferUtils.createFloatBuffer(16),
      clear = BufferUtils.createFloatBuffer(16);
  private final IntBuffer colorMask = BufferUtils.createIntBuffer(16);
  private final int[] textures = new int[2], samplers = new int[2];
  private final boolean[] texturing = new boolean[2];
  private int active, program, matrixMode, drawFbo, readFbo, vao, arrayBuffer, elementBuffer;
  private int srcRgb, dstRgb, srcAlpha, dstAlpha, equationRgb, equationAlpha;
  private boolean depth, cull, alpha, blend, lighting, scissored, depthMask, gl30, gl33, open;

  MapGlState begin() {
    if (open) throw new IllegalStateException("Nested use of the same map scope");
    open = true;
    active = GL11.glGetInteger(GL13.GL_ACTIVE_TEXTURE);
    program = GL11.glGetInteger(GL20.GL_CURRENT_PROGRAM);
    matrixMode = GL11.glGetInteger(GL11.GL_MATRIX_MODE);
    GL11.glPushClientAttrib(GL11.GL_CLIENT_VERTEX_ARRAY_BIT);
    gl30 = GLContext.getCapabilities().OpenGL30;
    gl33 = GLContext.getCapabilities().OpenGL33;
    if (gl30) {
      drawFbo = GL11.glGetInteger(GL30.GL_DRAW_FRAMEBUFFER_BINDING);
      readFbo = GL11.glGetInteger(GL30.GL_READ_FRAMEBUFFER_BINDING);
      vao = GL11.glGetInteger(GL30.GL_VERTEX_ARRAY_BINDING);
    }
    arrayBuffer = GL11.glGetInteger(GL15.GL_ARRAY_BUFFER_BINDING);
    elementBuffer = GL11.glGetInteger(GL15.GL_ELEMENT_ARRAY_BUFFER_BINDING);
    viewport.clear();
    GL11.glGetInteger(GL11.GL_VIEWPORT, viewport);
    scissor.clear();
    GL11.glGetInteger(GL11.GL_SCISSOR_BOX, scissor);
    colorMask.clear();
    GL11.glGetInteger(GL11.GL_COLOR_WRITEMASK, colorMask);
    color.clear();
    GL11.glGetFloat(GL11.GL_CURRENT_COLOR, color);
    clear.clear();
    GL11.glGetFloat(GL11.GL_COLOR_CLEAR_VALUE, clear);
    depth = GL11.glIsEnabled(GL11.GL_DEPTH_TEST);
    cull = GL11.glIsEnabled(GL11.GL_CULL_FACE);
    alpha = GL11.glIsEnabled(GL11.GL_ALPHA_TEST);
    blend = GL11.glIsEnabled(GL11.GL_BLEND);
    lighting = GL11.glIsEnabled(GL11.GL_LIGHTING);
    scissored = GL11.glIsEnabled(GL11.GL_SCISSOR_TEST);
    depthMask = GL11.glGetBoolean(GL11.GL_DEPTH_WRITEMASK);
    equationRgb = GL11.glGetInteger(GL20.GL_BLEND_EQUATION_RGB);
    equationAlpha = GL11.glGetInteger(GL20.GL_BLEND_EQUATION_ALPHA);
    srcRgb = GL11.glGetInteger(GL14.GL_BLEND_SRC_RGB);
    dstRgb = GL11.glGetInteger(GL14.GL_BLEND_DST_RGB);
    srcAlpha = GL11.glGetInteger(GL14.GL_BLEND_SRC_ALPHA);
    dstAlpha = GL11.glGetInteger(GL14.GL_BLEND_DST_ALPHA);
    for (int unit = 0; unit < 2; unit++) {
      active(GL13.GL_TEXTURE0 + unit);
      textures[unit] = GL11.glGetInteger(GL11.GL_TEXTURE_BINDING_2D);
      bound(textures[unit]);
      texturing[unit] = GL11.glIsEnabled(GL11.GL_TEXTURE_2D);
      if (gl33) {
        samplers[unit] = GL11.glGetInteger(GL33.GL_SAMPLER_BINDING);
        GL33.glBindSampler(unit, 0);
      }
      GL11.glMatrixMode(GL11.GL_TEXTURE);
      GL11.glPushMatrix();
      GL11.glLoadIdentity();
    }
    GL11.glDisable(GL11.GL_TEXTURE_2D);
    GlStateManager.disableTexture();
    active(GL13.GL_TEXTURE0);
    GL20.glBlendEquationSeparate(GL14.GL_FUNC_ADD, GL14.GL_FUNC_ADD);
    GL11.glColorMask(true, true, true, true);
    GlStateManager.colorMask(true, true, true, true);
    GL14.glBlendFuncSeparate(
        GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA, GL11.GL_ONE, GL11.GL_ONE_MINUS_SRC_ALPHA);
    GlStateManager.blendFuncSeparate(
        GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA, GL11.GL_ONE, GL11.GL_ONE_MINUS_SRC_ALPHA);
    GL11.glEnable(GL11.GL_TEXTURE_2D);
    GlStateManager.enableTexture();
    GL20.glUseProgram(0);
    GL11.glMatrixMode(GL11.GL_PROJECTION);
    GL11.glPushMatrix();
    GL11.glMatrixMode(GL11.GL_MODELVIEW);
    GL11.glPushMatrix();
    if (gl30) GL30.glBindVertexArray(0);
    GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, 0);
    GL15.glBindBuffer(GL15.GL_ELEMENT_ARRAY_BUFFER, 0);
    GL11.glDisableClientState(GL11.GL_COLOR_ARRAY);
    GL11.glDisableClientState(GL11.GL_NORMAL_ARRAY);
    GL13.glClientActiveTexture(GL13.GL_TEXTURE1);
    GL11.glDisableClientState(GL11.GL_TEXTURE_COORD_ARRAY);
    GL13.glClientActiveTexture(GL13.GL_TEXTURE0);
    GL11.glDisableClientState(GL11.GL_TEXTURE_COORD_ARRAY);
    GL11.glDisable(GL11.GL_DEPTH_TEST);
    GL11.glDisable(GL11.GL_CULL_FACE);
    GL11.glDisable(GL11.GL_ALPHA_TEST);
    GL11.glDisable(GL11.GL_LIGHTING);
    GlStateManager.disableLighting();
    GlStateManager.disableDepthTest();
    GlStateManager.disableCull();
    GlStateManager.disableAlphaTest();
    return this;
  }

  private static void active(int unit) {
    GL13.glActiveTexture(unit);
    GlStateManager.activeTexture(unit);
  }

  private static void bound(int texture) {
    GL11.glBindTexture(GL11.GL_TEXTURE_2D, texture);
    GlStateManager.bindTexture(texture);
  }

  private static void enabled(int capability, boolean value) {
    if (value) GL11.glEnable(capability);
    else GL11.glDisable(capability);
  }

  @Override
  public void close() {
    if (!open) return;
    open = false;
    GL20.glUseProgram(program);
    if (gl30) {
      GL30.glBindFramebuffer(GL30.GL_DRAW_FRAMEBUFFER, drawFbo);
      GL30.glBindFramebuffer(GL30.GL_READ_FRAMEBUFFER, readFbo);
      GL30.glBindVertexArray(vao);
    }
    GL11.glPopClientAttrib();
    GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, arrayBuffer);
    GL15.glBindBuffer(GL15.GL_ELEMENT_ARRAY_BUFFER, elementBuffer);
    GL11.glMatrixMode(GL11.GL_MODELVIEW);
    GL11.glPopMatrix();
    GL11.glMatrixMode(GL11.GL_PROJECTION);
    GL11.glPopMatrix();
    for (int unit = 0; unit < 2; unit++) {
      active(GL13.GL_TEXTURE0 + unit);
      GL11.glMatrixMode(GL11.GL_TEXTURE);
      GL11.glPopMatrix();
      bound(textures[unit]);
      enabled(GL11.GL_TEXTURE_2D, texturing[unit]);
      if (texturing[unit]) GlStateManager.enableTexture();
      else GlStateManager.disableTexture();
      if (gl33) GL33.glBindSampler(unit, samplers[unit]);
    }
    active(active);
    GL11.glMatrixMode(matrixMode);
    enabled(GL11.GL_DEPTH_TEST, depth);
    if (depth) GlStateManager.enableDepthTest();
    else GlStateManager.disableDepthTest();
    enabled(GL11.GL_CULL_FACE, cull);
    if (cull) GlStateManager.enableCull();
    else GlStateManager.disableCull();
    enabled(GL11.GL_ALPHA_TEST, alpha);
    if (alpha) GlStateManager.enableAlphaTest();
    else GlStateManager.disableAlphaTest();
    enabled(GL11.GL_BLEND, blend);
    if (blend) GlStateManager.enableBlend();
    else GlStateManager.disableBlend();
    enabled(GL11.GL_LIGHTING, lighting);
    if (lighting) GlStateManager.enableLighting();
    else GlStateManager.disableLighting();
    GL11.glColorMask(
        colorMask.get(0) != 0, colorMask.get(1) != 0, colorMask.get(2) != 0, colorMask.get(3) != 0);
    GlStateManager.colorMask(
        colorMask.get(0) != 0, colorMask.get(1) != 0, colorMask.get(2) != 0, colorMask.get(3) != 0);
    GL11.glDepthMask(depthMask);
    GlStateManager.depthMask(depthMask);
    GL20.glBlendEquationSeparate(equationRgb, equationAlpha);
    GL14.glBlendFuncSeparate(srcRgb, dstRgb, srcAlpha, dstAlpha);
    GlStateManager.blendFuncSeparate(srcRgb, dstRgb, srcAlpha, dstAlpha);
    GL11.glColor4f(color.get(0), color.get(1), color.get(2), color.get(3));
    GlStateManager.color4f(color.get(0), color.get(1), color.get(2), color.get(3));
    GL11.glClearColor(clear.get(0), clear.get(1), clear.get(2), clear.get(3));
    GlStateManager.clearColor(clear.get(0), clear.get(1), clear.get(2), clear.get(3));
    GL11.glViewport(viewport.get(0), viewport.get(1), viewport.get(2), viewport.get(3));
    enabled(GL11.GL_SCISSOR_TEST, scissored);
    GL11.glScissor(scissor.get(0), scissor.get(1), scissor.get(2), scissor.get(3));
  }
}
