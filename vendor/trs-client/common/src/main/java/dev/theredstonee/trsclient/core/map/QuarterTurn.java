package dev.theredstonee.trsclient.core.map;

/** Exact cardinal camera transforms. Block transforms include the reflected cell's far edge. */
public final class QuarterTurn {
  public static int normalize(int q) {
    return q & 3;
  }

  public static double u(double x, double z, int q) {
    switch (q & 3) {
      case 1:
        return -z;
      case 2:
        return -x;
      case 3:
        return z;
      default:
        return x;
    }
  }

  public static double depth(double x, double z, int q) {
    switch (q & 3) {
      case 1:
        return x;
      case 2:
        return -z;
      case 3:
        return -x;
      default:
        return z;
    }
  }

  public static int blockU(int x, int z, int q) {
    switch (q & 3) {
      case 1:
        return -z - 1;
      case 2:
        return -x - 1;
      case 3:
        return z;
      default:
        return x;
    }
  }

  public static int blockDepth(int x, int z, int q) {
    switch (q & 3) {
      case 1:
        return x;
      case 2:
        return -z - 1;
      case 3:
        return -x - 1;
      default:
        return z;
    }
  }

  public static double x(double u, double d, int q) {
    return u(u, d, -q);
  }

  public static double z(double u, double d, int q) {
    return depth(u, d, -q);
  }

  public static int blockX(int u, int d, int q) {
    return blockU(u, d, -q);
  }

  public static int blockZ(int u, int d, int q) {
    return blockDepth(u, d, -q);
  }

  public static int[] box(int[] b, int q) {
    if (b == null) return null;
    if (b.length < 6) throw new IllegalArgumentException("World box needs six coordinates");
    int x0 = blockU(b[0], b[2], q),
        z0 = blockDepth(b[0], b[2], q),
        x1 = blockU(b[3], b[5], q),
        z1 = blockDepth(b[3], b[5], q);
    int[] result = b.clone();
    result[0] = Math.min(x0, x1);
    result[2] = Math.min(z0, z1);
    result[3] = Math.max(x0, x1);
    result[5] = Math.max(z0, z1);
    return result;
  }

  private QuarterTurn() {}
}
