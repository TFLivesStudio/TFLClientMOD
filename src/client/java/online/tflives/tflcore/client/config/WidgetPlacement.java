package online.tflives.tflcore.client.config;

/**
 * Persisted layout of a single HUD widget.
 *
 * <p>Positions are stored as offsets from an anchor, which makes layouts survive GUI scale and
 * resolution changes. Values are always normalized on load so a hand-edited file can never put a
 * widget outside a sane range.</p>
 */
public final class WidgetPlacement {
    public static final String TOP_LEFT = "TOP_LEFT";
    public static final String TOP_RIGHT = "TOP_RIGHT";
    public static final String BOTTOM_LEFT = "BOTTOM_LEFT";
    public static final String BOTTOM_RIGHT = "BOTTOM_RIGHT";
    public static final String CENTER = "CENTER";
    public static final String TOP_CENTER = "TOP_CENTER";
    public static final String BOTTOM_CENTER = "BOTTOM_CENTER";

    public boolean enabled = true;
    public String anchor = TOP_LEFT;
    public int x = 6;
    public int y = 6;
    public float scale = 1.0F;
    public boolean background = true;
    public boolean compact = false;
    public float opacity = 0.88F;

    public WidgetPlacement() {
    }

    public WidgetPlacement(boolean enabled, String anchor, int x, int y, float scale, boolean background) {
        this.enabled = enabled;
        this.anchor = anchor;
        this.x = x;
        this.y = y;
        this.scale = scale;
        this.background = background;
    }

    public WidgetPlacement copy() {
        WidgetPlacement copy = new WidgetPlacement();
        copy.enabled = enabled;
        copy.anchor = anchor;
        copy.x = x;
        copy.y = y;
        copy.scale = scale;
        copy.background = background;
        copy.compact = compact;
        copy.opacity = opacity;
        return copy;
    }

    public void normalize() {
        if (anchor == null || !isKnownAnchor(anchor)) {
            anchor = TOP_LEFT;
        }
        x = Math.max(-2048, Math.min(x, 2048));
        y = Math.max(-2048, Math.min(y, 2048));
        if (!Float.isFinite(scale)) {
            scale = 1.0F;
        }
        scale = Math.max(0.5F, Math.min(scale, 3.0F));
        if (!Float.isFinite(opacity)) {
            opacity = 0.88F;
        }
        opacity = Math.max(0.15F, Math.min(opacity, 1.0F));
    }

    public static boolean isKnownAnchor(String value) {
        return TOP_LEFT.equals(value) || TOP_RIGHT.equals(value)
                || BOTTOM_LEFT.equals(value) || BOTTOM_RIGHT.equals(value)
                || CENTER.equals(value) || TOP_CENTER.equals(value) || BOTTOM_CENTER.equals(value);
    }
}
