package dev.theredstonee.trsclient.ornithe;

import dev.theredstonee.trsclient.core.branding.BrandIcons;
import java.nio.ByteBuffer;
import org.lwjgl.opengl.Display;

public final class WindowIdentity {
  private static boolean installed;

  private WindowIdentity() {}

  public static void refresh() {
    if (!Display.isCreated()) return;
    String title = dev.theredstonee.trsclient.core.product.Product.displayName("1.8.9");
    if (!title.equals(Display.getTitle())) Display.setTitle(title);
    if (installed) return;
    try { // SDLDisplay 1.4.10 copies every alternate icon into one LWJGL MemoryStack.
      // Keep that combined allocation below 64 KiB; larger variants remain shipped for launchers.
      int[] sizes = {64, 48, 32, 24, 16};
      ByteBuffer[] icons = new ByteBuffer[sizes.length];
      for (int i = 0; i < icons.length; i++) {
        int n = sizes[i];
        ByteBuffer b = org.lwjgl.BufferUtils.createByteBuffer(n * n * 4);
        for (int p : BrandIcons.pixels(n))
          b.put((byte) (p >> 16)).put((byte) (p >> 8)).put((byte) p).put((byte) (p >>> 24));
        b.flip();
        icons[i] = b;
      }
      Display.setIcon(icons);
      installed = true;
    } catch (RuntimeException error) {
      installed = true;
      OrnitheClient.LOG.warn("Window icon unavailable", error);
    }
  }

  public static void windowChanged() {
    installed = false;
    refresh();
  }
}
