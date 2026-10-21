package online.tflives.tflcore.client.ui;

import online.tflives.tflcore.client.config.TflSettings;

/** Design tokens shared by the launcher-style menu, configuration and HUD. */
public record TflTheme(int top, int bottom, int panel, int accent, int text, int muted) {
    public static TflTheme from(TflSettings settings) {
        return switch (settings.theme) {
            case "MIDNIGHT" -> new TflTheme(0xFF090B15, 0xFF171E32, 0xDF121728,
                    0xFF8F9AFF, 0xFFF7F8FF, 0xFFB0B5CA);
            case "FROST" -> new TflTheme(0xFF1D3A58, 0xFF7195B3, 0xDF132E49,
                    0xFF9CEBFF, 0xFFFFFFFF, 0xFFE3F2FD);
            default -> new TflTheme(0xFF05152E, 0xFF133D75, 0xE0092347,
                    0xFF35A9FF, 0xFFFFFFFF, 0xFFBBD5F5);
        };
    }
}
