package dev.theredstonee.trsclient.ornithe;

import dev.theredstonee.trsclient.core.ui.*;
import org.lwjgl.opengl.*;

/** Supersampled owned map target; the final shader clips and antialiases the circular edge. */
final class DioramaMinimapTarget implements AutoCloseable {
  private final MapGlState state = new MapGlState();
  private int fbo, texture, edge, program;
  private long updated, renderCost;
  private boolean failed;

  void draw(
      OrnitheClient client, Canvas canvas, int size, boolean round, int opacity, Runnable render) {
    if (failed || !GLContext.getCapabilities().OpenGL30) {
      render.run();
      return;
    }
    int pixels = Math.min(512, size * 2),
        oldFramebuffer = GL11.glGetInteger(GL30.GL_FRAMEBUFFER_BINDING);
    int oldProgram = GL11.glGetInteger(GL20.GL_CURRENT_PROGRAM);
    try (var ignored = state.begin()) {
      if (edge != pixels) {
        releaseTarget();
        edge = pixels;
        fbo = GL30.glGenFramebuffers();
        texture = GL11.glGenTextures();
        GL30.glBindFramebuffer(GL30.GL_FRAMEBUFFER, fbo);
        net.minecraft.client.render.platform.GlStateManager.bindTexture(texture);
        GL11.glTexImage2D(
            GL11.GL_TEXTURE_2D,
            0,
            GL11.GL_RGBA8,
            edge,
            edge,
            0,
            GL11.GL_RGBA,
            GL11.GL_UNSIGNED_BYTE,
            (java.nio.ByteBuffer) null);
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MIN_FILTER, GL11.GL_LINEAR);
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MAG_FILTER, GL11.GL_LINEAR);
        GL30.glFramebufferTexture2D(
            GL30.GL_FRAMEBUFFER, GL30.GL_COLOR_ATTACHMENT0, GL11.GL_TEXTURE_2D, texture, 0);
        if (GL30.glCheckFramebufferStatus(GL30.GL_FRAMEBUFFER) != GL30.GL_FRAMEBUFFER_COMPLETE)
          throw new IllegalStateException("Minimap framebuffer incomplete");
      }
      // Retain 60 Hz at normal cost; expensive targets adapt to 30 Hz without reducing quality.
      long interval = Math.max(1_000_000_000L / 60, Math.min(1_000_000_000L / 30, renderCost * 2));
      if (System.nanoTime() - updated > interval) {
        long began = System.nanoTime();
        GL30.glBindFramebuffer(GL30.GL_FRAMEBUFFER, fbo);
        GL11.glViewport(0, 0, edge, edge);
        GL11.glDisable(GL11.GL_SCISSOR_TEST);
        net.minecraft.client.render.platform.GlStateManager.disableDepthTest();
        GL11.glClearColor(0, 0, 0, 0);
        GL11.glClear(GL11.GL_COLOR_BUFFER_BIT);
        GL11.glMatrixMode(GL11.GL_PROJECTION);
        GL11.glPushMatrix();
        GL11.glLoadIdentity();
        GL11.glOrtho(0, size, size, 0, -1000, 1000);
        GL11.glMatrixMode(GL11.GL_MODELVIEW);
        GL11.glPushMatrix();
        GL11.glLoadIdentity();
        try {
          client.canvas.withSurface(size, size, edge / size, render);
          updated = System.nanoTime();
          renderCost = renderCost == 0 ? updated - began : (renderCost * 7 + updated - began) / 8;
        } finally {
          GL11.glPopMatrix();
          GL11.glMatrixMode(GL11.GL_PROJECTION);
          GL11.glPopMatrix();
          GL11.glMatrixMode(GL11.GL_MODELVIEW);
        }
      }
    } catch (RuntimeException | LinkageError error) {
      failed = true;
      OrnitheClient.LOG.warn("Native minimap target unavailable", error);
    }
    if (failed) {
      render.run();
      return;
    }
    try (var ignored = state.begin()) {
      if (program == 0) program = createProgram();
      GL20.glUseProgram(program);
      GL20.glUniform1i(GL20.glGetUniformLocation(program, "Map"), 0);
      GL20.glUniform1f(GL20.glGetUniformLocation(program, "Round"), round ? 1 : 0);
      GL20.glUniform1f(GL20.glGetUniformLocation(program, "Opacity"), opacity / 100f);
      net.minecraft.client.render.platform.GlStateManager.disableCull();
      // Compose our owned target directly; do not enter native font/GUI batching with this shader.
      GL11.glDisable(GL11.GL_SCISSOR_TEST);
      GL11.glEnable(GL11.GL_TEXTURE_2D);
      GL11.glEnable(GL11.GL_BLEND);
      net.minecraft.client.render.platform.GlStateManager.enableTexture();
      net.minecraft.client.render.platform.GlStateManager.enableBlend();
      GL14.glBlendFuncSeparate(
          GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA, GL11.GL_ONE, GL11.GL_ONE_MINUS_SRC_ALPHA);
      net.minecraft.client.render.platform.GlStateManager.blendFuncSeparate(
          GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA, GL11.GL_ONE, GL11.GL_ONE_MINUS_SRC_ALPHA);
      GL11.glBindTexture(GL11.GL_TEXTURE_2D, texture);
      net.minecraft.client.render.platform.GlStateManager.bindTexture(texture);
      GL11.glBegin(GL11.GL_QUADS);
      GL11.glTexCoord2f(0, 1);
      GL11.glVertex2f(0, 0);
      GL11.glTexCoord2f(1, 1);
      GL11.glVertex2f(size, 0);
      GL11.glTexCoord2f(1, 0);
      GL11.glVertex2f(size, size);
      GL11.glTexCoord2f(0, 0);
      GL11.glVertex2f(0, size);
      GL11.glEnd();
    } catch (RuntimeException | LinkageError e) {
      failed = true;
      OrnitheClient.LOG.warn("Minimap clipping unavailable", e);
    } finally {
      GL20.glUseProgram(oldProgram);
    }
  }

  private int createProgram() {
    int v =
        compile(
            GL20.GL_VERTEX_SHADER,
            "#version 120\n"
                + "varying vec2 uv;void"
                + " main(){gl_Position=gl_ModelViewProjectionMatrix*gl_Vertex;uv=gl_MultiTexCoord0.xy;}");
    int f =
        compile(
            GL20.GL_FRAGMENT_SHADER,
            "#version 120\n"
                + "uniform sampler2D Map;uniform float Round;uniform float Opacity;varying vec2"
                + " uv;void main(){vec4 c=texture2D(Map,uv);float"
                + " edge=Round<0.5?1.0:1.0-smoothstep(0.485,0.5,length(uv-vec2(0.5)));gl_FragColor=vec4(c.rgb,c.a*edge*Opacity);}");
    int p = GL20.glCreateProgram();
    try {
      GL20.glAttachShader(p, v);
      GL20.glAttachShader(p, f);
      GL20.glLinkProgram(p);
      if (GL20.glGetProgrami(p, GL20.GL_LINK_STATUS) == 0)
        throw new IllegalStateException(GL20.glGetProgramInfoLog(p, 4096));
      return p;
    } finally {
      GL20.glDeleteShader(v);
      GL20.glDeleteShader(f);
    }
  }

  private int compile(int type, String source) {
    int shader = GL20.glCreateShader(type);
    GL20.glShaderSource(shader, source);
    GL20.glCompileShader(shader);
    if (GL20.glGetShaderi(shader, GL20.GL_COMPILE_STATUS) == 0)
      throw new IllegalStateException(GL20.glGetShaderInfoLog(shader, 4096));
    return shader;
  }

  private void releaseTarget() {
    if (fbo != 0) GL30.glDeleteFramebuffers(fbo);
    if (texture != 0) GL11.glDeleteTextures(texture);
    fbo = texture = 0;
    edge = 0;
  }

  public void close() {
    releaseTarget();
    if (program != 0) GL20.glDeleteProgram(program);
    program = 0;
    updated = renderCost = 0;
  }
}
