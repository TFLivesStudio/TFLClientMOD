package online.tflives.tflcore.client.hud;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import online.tflives.tflcore.client.config.TflSettings;
import online.tflives.tflcore.client.config.WidgetPlacement;
import online.tflives.tflcore.client.ui.TflTheme;

import java.util.ArrayList;
import java.util.List;

/**
 * Base class for HUD widgets.
 *
 * <p>Widgets cache their text and only resample when their refresh interval elapses. Rendering
 * itself is stateless and allocation-free in the common path; this keeps a full HUD cheap even with
 * dozens of widgets active.</p>
 */
public abstract class HudWidget {
    public static final int PAD_X = 6;
    public static final int PAD_Y = 4;
    public static final int LINE_HEIGHT = 11;

    private final String id;
    private final WidgetPlacement defaults;
    private final List<String> lines = new ArrayList<>(4);
    protected int contentWidth;
    protected int contentHeight;
    private long refreshIntervalNanos = 250_000_000L;
    private long lastRefresh = Long.MIN_VALUE;
    private boolean dirty = true;

    protected HudWidget(String id, boolean defaultEnabled, String anchor, int x, int y) {
        this.id = id;
        this.defaults = new WidgetPlacement(defaultEnabled, anchor, x, y, 1.0F, true);
    }

    public final String id() {
        return id;
    }

    public String nameKey() {
        return "tflcore.hud.widget." + id;
    }

    public final WidgetPlacement defaultPlacement() {
        return defaults.copy();
    }

    public final long refreshIntervalNanos() {
        return refreshIntervalNanos;
    }

    protected final void setRefreshIntervalNanos(long nanos) {
        this.refreshIntervalNanos = Math.max(16_000_000L, nanos);
    }

    /** Marks cached content stale (used by toggles that change what the widget shows). */
    public final void invalidate() {
        dirty = true;
    }

    /** Whether the widget has meaningful data right now. */
    public boolean isAvailable(Minecraft client, TflSettings settings) {
        return client.player != null;
    }

    public final void updateIfNeeded(Minecraft client, TflSettings settings, long nowNanos) {
        if (!dirty && nowNanos - lastRefresh < refreshIntervalNanos) {
            return;
        }
        lines.clear();
        contentWidth = 0;
        contentHeight = 0;
        try {
            collect(client, settings);
        } catch (RuntimeException ignored) {
            lines.clear();
            contentWidth = 0;
            contentHeight = 0;
        }
        contentHeight = Math.max(contentHeight, lines.size() * LINE_HEIGHT);
        for (int i = 0; i < lines.size(); i++) {
            contentWidth = Math.max(contentWidth, client.font.width(lines.get(i)));
        }
        lastRefresh = nowNanos;
        dirty = false;
    }

    /** Rebuilds the cached content. Called at most once per refresh interval. */
    protected abstract void collect(Minecraft client, TflSettings settings);

    protected final void line(String text) {
        if (text != null) {
            lines.add(text);
        }
    }

    protected final void blank() {
        lines.add("");
    }

    protected final List<String> lines() {
        return lines;
    }

    public final int width() {
        return contentWidth + PAD_X * 2;
    }

    public final int height() {
        return contentHeight + PAD_Y * 2;
    }

    /** Default text renderer; custom widgets override this. */
    public void render(GuiGraphicsExtractor graphics, Minecraft client, TflTheme theme, TflSettings settings,
                       WidgetPlacement placement, int width, int height) {
        int y = PAD_Y;
        for (int i = 0; i < lines.size(); i++) {
            graphics.text(client.font, lines.get(i), PAD_X, y, theme.text(), true);
            y += LINE_HEIGHT;
        }
    }

    /** Masking helper for streamer mode. */
    protected static String mask(String value) {
        return value == null || value.isEmpty() ? "***" : "***";
    }

    protected static String stripNamespace(String id) {
        if (id == null) return "";
        int colon = id.indexOf(':');
        return colon >= 0 ? id.substring(colon + 1) : id;
    }

    protected static String titleCase(String id) {
        String value = stripNamespace(id).replace('_', ' ');
        if (value.isEmpty()) return value;
        StringBuilder builder = new StringBuilder(value.length());
        boolean upper = true;
        for (int i = 0; i < value.length(); i++) {
            char c = value.charAt(i);
            if (c == ' ') {
                upper = true;
                builder.append(c);
            } else if (upper) {
                builder.append(Character.toUpperCase(c));
                upper = false;
            } else {
                builder.append(c);
            }
        }
        return builder.toString();
    }
}
