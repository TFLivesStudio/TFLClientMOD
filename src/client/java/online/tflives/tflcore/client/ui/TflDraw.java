package online.tflives.tflcore.client.ui;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;

/** Small drawing helpers shared by every TFL screen. No direct OpenGL, only the extractor API. */
public final class TflDraw {
    private TflDraw() {
    }

    public static void panel(GuiGraphicsExtractor graphics, TflTheme theme, int x, int y, int width, int height,
                             boolean hovered, boolean focused) {
        if (width <= 0 || height <= 0) return;
        graphics.fill(x + 1, y + 2, x + width + 1, y + height + 2, theme.shadow());
        graphics.fill(x, y, x + width, y + height, hovered ? theme.surfaceHover() : theme.surface());
        graphics.outline(x, y, width, height, focused ? theme.borderStrong() : theme.border());
        if (hovered || focused) {
            graphics.fill(x, y, x + 2, y + height, theme.accent());
        }
    }

    public static void card(GuiGraphicsExtractor graphics, TflTheme theme, int x, int y, int width, int height) {
        graphics.fill(x, y, x + width, y + height, theme.surface());
        graphics.outline(x, y, width, height, theme.border());
    }

    public static void divider(GuiGraphicsExtractor graphics, TflTheme theme, int x, int y, int width) {
        graphics.fill(x, y, x + width, y + 1, theme.border());
    }

    public static void text(GuiGraphicsExtractor graphics, Font font, String text, int x, int y, int color) {
        graphics.text(font, text, x, y, color, false);
    }

    public static void textShadowed(GuiGraphicsExtractor graphics, Font font, String text, int x, int y, int color) {
        graphics.text(font, text, x, y, color, true);
    }

    public static void centered(GuiGraphicsExtractor graphics, Font font, String text, int centerX, int y, int color) {
        graphics.centeredText(font, text, centerX, y, color);
    }

    public static void rightAligned(GuiGraphicsExtractor graphics, Font font, String text, int rightX, int y, int color) {
        graphics.text(font, text, rightX - font.width(text), y, color, false);
    }

    /** Draws a label clipped to a box, so long translations never bleed into a button border. */
    public static void clippedLabel(GuiGraphicsExtractor graphics, Font font, String text, int x, int y,
                                    int maxWidth, int color, boolean shadow) {
        graphics.enableScissor(x, y - 2, x + Math.max(0, maxWidth), y + font.lineHeight + 2);
        graphics.text(font, text, x, y, color, shadow);
        graphics.disableScissor();
    }

    public static void progressBar(GuiGraphicsExtractor graphics, TflTheme theme, int x, int y, int width,
                                   int height, float value) {
        float clamped = Math.max(0F, Math.min(value, 1F));
        graphics.fill(x, y, x + width, y + height, theme.accentGlow());
        int filled = Math.round(width * clamped);
        if (filled > 0) {
            graphics.fill(x, y, x + filled, y + height, theme.accent());
        }
        graphics.outline(x, y, width, height, theme.border());
    }

    public static void pill(GuiGraphicsExtractor graphics, TflTheme theme, int x, int y, int width, int height,
                            boolean active) {
        graphics.fill(x, y, x + width, y + height, active ? theme.accentSoft() : theme.surface());
        graphics.outline(x, y, width, height, active ? theme.borderStrong() : theme.border());
    }

    public static void cornerBrackets(GuiGraphicsExtractor graphics, int color, int x, int y, int width, int height,
                                      int length) {
        graphics.fill(x, y, x + length, y + 1, color);
        graphics.fill(x, y, x + 1, y + length, color);
        graphics.fill(x + width - length, y, x + width, y + 1, color);
        graphics.fill(x + width - 1, y, x + width, y + length, color);
        graphics.fill(x, y + height - 1, x + length, y + height, color);
        graphics.fill(x, y + height - length, x + 1, y + height, color);
        graphics.fill(x + width - length, y + height - 1, x + width, y + height, color);
        graphics.fill(x + width - 1, y + height - length, x + width, y + height, color);
    }

    public static String trim(Font font, String text, int maxWidth) {
        if (text == null || maxWidth <= 0) return "";
        if (font.width(text) <= maxWidth) return text;
        String ellipsis = "...";
        int available = maxWidth - font.width(ellipsis);
        if (available <= 0) return "";
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < text.length(); i++) {
            if (font.width(builder.toString() + text.charAt(i)) > available) break;
            builder.append(text.charAt(i));
        }
        return builder + ellipsis;
    }

    public static int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(value, max));
    }

    public static float clamp(float value, float min, float max) {
        return Math.max(min, Math.min(value, max));
    }
}
