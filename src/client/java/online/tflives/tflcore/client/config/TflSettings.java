package online.tflives.tflcore.client.config;

import java.util.Locale;

/**
 * Versioned, local-only configuration. Never persist credentials or telemetry.
 * All fields are simple JSON values so future versions can migrate them safely.
 */
public final class TflSettings {
    public static final int SCHEMA_VERSION = 1;

    public int schemaVersion = SCHEMA_VERSION;
    public boolean customMainMenu = true;
    public boolean animatedBackground = true;
    public boolean reducedMotion = false;
    public boolean hudEnabled = true;
    public boolean showFps = true;
    public boolean showCoordinates = true;
    public boolean showClock = false;
    public boolean telemetryConsent = false;
    public String theme = "BLUE";
    public String preset = "BALANCED";
    public int hudX = 8;
    public int hudY = 8;

    public void normalize() {
        schemaVersion = SCHEMA_VERSION;
        if (theme == null || !isOneOf(theme, "BLUE", "MIDNIGHT", "FROST")) {
            theme = "BLUE";
        } else {
            theme = theme.toUpperCase(Locale.ROOT);
        }
        if (preset == null || !isOneOf(preset, "BALANCED", "FPS", "QUALITY", "CUSTOM")) {
            preset = "BALANCED";
        } else {
            preset = preset.toUpperCase(Locale.ROOT);
        }
        hudX = Math.max(0, Math.min(hudX, 10000));
        hudY = Math.max(0, Math.min(hudY, 10000));
    }

    private static boolean isOneOf(String value, String... options) {
        for (String option : options) {
            if (option.equalsIgnoreCase(value)) return true;
        }
        return false;
    }

    /** Only changes TFL-owned effects and modules, never overrides external mod settings. */
    public void applyPreset(String next) {
        switch (next.toUpperCase(Locale.ROOT)) {
            case "FPS" -> {
                preset = "FPS";
                animatedBackground = false;
                reducedMotion = true;
                hudEnabled = true;
                showClock = false;
            }
            case "QUALITY" -> {
                preset = "QUALITY";
                animatedBackground = true;
                reducedMotion = false;
                hudEnabled = true;
            }
            default -> {
                preset = "BALANCED";
                animatedBackground = true;
                reducedMotion = false;
                hudEnabled = true;
            }
        }
    }
}
