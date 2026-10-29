package dev.theredstonee.trsclient.core.branding;

import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.*;
import java.util.*;
import javax.imageio.ImageIO;

/** Exact supplied logo, proportionally resampled on black; no crop or geometry reconstruction. */
public final class BrandIcons {
  public static final int[] SIZES = {16, 24, 32, 48, 64, 128, 256};
  private static final Map<Integer, int[]> ICONS = new HashMap<Integer, int[]>();

  private BrandIcons() {}

  public static synchronized int[] pixels(int size) {
    if (!ICONS.containsKey(size))
      try (InputStream in =
          BrandIcons.class.getResourceAsStream("/assets/trsclient/branding/logo-original.png")) {
        if (in == null) throw new IOException("Missing original TFL logo");
        BufferedImage original = ImageIO.read(in);
        if (original == null) throw new IOException("Invalid TFL logo");
        for (int n : SIZES) {
          BufferedImage image = new BufferedImage(n, n, BufferedImage.TYPE_INT_ARGB);
          Graphics2D g = image.createGraphics();
          try {
            g.setColor(java.awt.Color.BLACK);
            g.fillRect(0, 0, n, n);
            g.setRenderingHint(
                RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
            double scale =
                Math.min(n / (double) original.getWidth(), n / (double) original.getHeight());
            int w = Math.max(1, (int) Math.round(original.getWidth() * scale)),
                h = Math.max(1, (int) Math.round(original.getHeight() * scale));
            g.drawImage(original, (n - w) / 2, (n - h) / 2, w, h, null);
          } finally {
            g.dispose();
          }
          ICONS.put(n, image.getRGB(0, 0, n, n, null, 0, n));
        }
      } catch (IOException e) {
        throw new IllegalStateException("Cannot prepare TFL icons", e);
      }
    int[] pixels = ICONS.get(size);
    if (pixels == null) throw new IllegalArgumentException("Unsupported icon size");
    return pixels;
  }

  /** Build-time asset exporter; the original PNG remains untouched. */
  public static void main(String[] args) throws IOException {
    File directory = new File(args[0]);
    if (!directory.isDirectory() && !directory.mkdirs())
      throw new IOException("Cannot create icon directory");
    for (int size : SIZES) {
      BufferedImage image = new BufferedImage(size, size, BufferedImage.TYPE_INT_ARGB);
      image.setRGB(0, 0, size, size, pixels(size), 0, size);
      ImageIO.write(image, "png", new File(directory, "icon-" + size + ".png"));
    }
  }
}
