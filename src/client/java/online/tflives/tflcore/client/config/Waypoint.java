package online.tflives.tflcore.client.config;

/**
 * Personal, local-only waypoint. Nothing is ever uploaded; the world key only exists so waypoints
 * can be filtered by dimension and server.
 */
public final class Waypoint {
    public static final int MAX_NAME_LENGTH = 48;
    public static final int DEFAULT_COLOR = 0xFF35A9FF;

    public String name = "";
    public String dimension = "";
    public String world = "";
    public double x;
    public double y;
    public double z;
    public int color = DEFAULT_COLOR;
    public boolean enabled = true;
    public boolean death = false;

    public Waypoint() {
    }

    public Waypoint(String name, String world, String dimension, double x, double y, double z) {
        this.name = name;
        this.world = world;
        this.dimension = dimension;
        this.x = x;
        this.y = y;
        this.z = z;
    }

    public double distanceTo(double px, double py, double pz) {
        double dx = x - px;
        double dy = y - py;
        double dz = z - pz;
        return Math.sqrt(dx * dx + dy * dy + dz * dz);
    }

    /** Horizontal distance, the useful number when walking towards a target. */
    public double horizontalDistanceTo(double px, double pz) {
        double dx = x - px;
        double dz = z - pz;
        return Math.sqrt(dx * dx + dz * dz);
    }

    public void normalize() {
        if (name == null) {
            name = "";
        }
        name = name.strip();
        if (name.length() > MAX_NAME_LENGTH) {
            name = name.substring(0, MAX_NAME_LENGTH);
        }
        if (world == null) {
            world = "";
        }
        if (dimension == null) {
            dimension = "";
        }
        if (!Double.isFinite(x)) {
            x = 0;
        }
        if (!Double.isFinite(y)) {
            y = 0;
        }
        if (!Double.isFinite(z)) {
            z = 0;
        }
        color |= 0xFF000000;
    }

    public Waypoint copy() {
        Waypoint copy = new Waypoint(name, world, dimension, x, y, z);
        copy.color = color;
        copy.enabled = enabled;
        copy.death = death;
        return copy;
    }
}
