package online.tflives.tflcore.client.ui;

import online.tflives.tflcore.client.config.TflSettings;

/**
 * Central design tokens for every TFL Core surface.
 *
 * <p>All screens and widgets read colors from here, so a theme change is applied consistently to
 * the main menu, settings, HUD and editor. Tokens are plain ARGB integers.</p>
 */
public record TflTheme(
        String id,
        int bgTop,
        int bgBottom,
        int surface,
        int surfaceHover,
        int surfaceActive,
        int border,
        int borderStrong,
        int accent,
        int accentSoft,
        int accentGlow,
        int text,
        int muted,
        int danger,
        int success,
        int warning,
        int shadow) {

    private static final int WHITE = 0xFFFFFFFF;

    public static TflTheme from(TflSettings settings) {
        String theme = settings == null || settings.ui == null ? "BLUE" : settings.ui.theme;
        String accent = settings == null || settings.ui == null ? "DEFAULT" : settings.ui.accent;
        TflTheme base = switch (theme == null ? "BLUE" : theme) {
            case "MIDNIGHT" -> build("MIDNIGHT", 0xFF05060C, 0xFF121A2E, 0xFF8F9AFF, 0xFFF7F8FF);
            case "FROST" -> build("FROST", 0xFF0E2438, 0xFF3D6B8C, 0xFF9CEBFF, 0xFFFFFFFF);
            case "EMBER" -> build("EMBER", 0xFF170A08, 0xFF3B1710, 0xFFFF7A45, 0xFFFFF4EE);
            case "MONO" -> build("MONO", 0xFF08080A, 0xFF1B1B20, 0xFFE6E6EA, 0xFFF4F4F7);
            default -> build("BLUE", 0xFF051224, 0xFF0B2B57, 0xFF35A9FF, 0xFFFFFFFF);
        };
        if (accent == null || "DEFAULT".equals(accent)) {
            return reduceTransparency(settings, base);
        }
        int color = switch (accent) {
            case "CYAN" -> 0xFF35E0DF;
            case "VIOLET" -> 0xFFA87CFF;
            case "AMBER" -> 0xFFFFB648;
            case "LIME" -> 0xFF7CE07C;
            case "ROSE" -> 0xFFFF6E9C;
            default -> base.accent();
        };
        return reduceTransparency(settings, base.withAccent(color));
    }

    private static TflTheme reduceTransparency(TflSettings settings, TflTheme theme) {
        if (settings == null || settings.ui == null || !settings.ui.reducedTransparency) {
            return theme;
        }
        return theme.withOpaqueSurfaces();
    }

    /** Makes panel surfaces fully opaque so text stays legible over busy scenes. */
    public TflTheme withOpaqueSurfaces() {
        return new TflTheme(id, bgTop, bgBottom, argb(0xFF, surface), argb(0xFF, surfaceHover),
                surfaceActive, border, borderStrong, accent, accentSoft, accentGlow, text, muted,
                danger, success, warning, shadow);
    }

    public TflTheme withAccent(int color) {
        return build(id, bgTop, bgBottom, color, text);
    }

    private static TflTheme build(String id, int bgTop, int bgBottom, int accent, int text) {
        int surface = argb(0xE6, blend(bgTop, accent, 0.07F));
        int surfaceHover = argb(0xF2, blend(bgBottom, accent, 0.18F));
        int surfaceActive = argb(0xFF, blend(bgBottom, accent, 0.30F));
        int border = argb(0x4D, accent);
        int borderStrong = argb(0xAA, accent);
        int accentSoft = argb(0x33, accent);
        int accentGlow = argb(0x24, accent);
        int muted = argb(0xFF, blend(bgBottom, WHITE, 0.66F));
        return new TflTheme(id, bgTop, bgBottom, surface, surfaceHover, surfaceActive,
                border, borderStrong, accent, accentSoft, accentGlow, text, muted,
                0xFFFF5C6C, 0xFF58D68D, 0xFFFFC24B, 0x8C000000);
    }

    /** Applies a replacement text color (used by high-contrast mode). */
    public TflTheme withText(int textColor, int mutedColor) {
        return new TflTheme(id, bgTop, bgBottom, surface, surfaceHover, surfaceActive,
                border, borderStrong, accent, accentSoft, accentGlow, textColor, mutedColor,
                danger, success, warning, shadow);
    }

    public int surfaceWithAlpha(float opacity) {
        return argb(Math.round(Math.max(0F, Math.min(opacity, 1F)) * 255F), surface);
    }

    public static int argb(int alpha, int rgb) {
        return (alpha & 0xFF) << 24 | (rgb & 0xFFFFFF);
    }

    public static int withAlpha(int color, float opacity) {
        int alpha = Math.round(Math.max(0F, Math.min(opacity, 1F)) * 255F);
        return argb(alpha, color);
    }

    /** Linearly mixes two ARGB colors; alpha of {@code from} is kept. */
    public static int blend(int from, int to, float amount) {
        float t = Math.max(0F, Math.min(amount, 1F));
        int r = Math.round(((from >> 16) & 0xFF) + (((to >> 16) & 0xFF) - ((from >> 16) & 0xFF)) * t);
        int g = Math.round(((from >> 8) & 0xFF) + (((to >> 8) & 0xFF) - ((from >> 8) & 0xFF)) * t);
        int b = Math.round((from & 0xFF) + ((to & 0xFF) - (from & 0xFF)) * t);
        int a = (from >>> 24) == 0 ? 0xFF : (from >>> 24);
        return (a << 24) | (r << 16) | (g << 8) | b;
    }

    /** Ease-out curve used for hover/focus transitions (0..1 in, 0..1 out). */
    public static float ease(float value) {
        float x = Math.max(0F, Math.min(value, 1F));
        return 1F - (1F - x) * (1F - x) * (1F - x);
    }
}
