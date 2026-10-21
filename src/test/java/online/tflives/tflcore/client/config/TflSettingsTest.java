package online.tflives.tflcore.client.config;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

final class TflSettingsTest {
    @Test
    void defaultsKeepTelemetryOffAndMenuEnabled() {
        TflSettings s = new TflSettings();
        assertFalse(s.telemetryConsent);
        assertTrue(s.customMainMenu);
        assertEquals("BLUE", s.theme);
        assertEquals("BALANCED", s.preset);
    }

    @Test
    void invalidConfigIsNormalizedAndOffsetsClamped() {
        TflSettings s = new TflSettings();
        s.theme = "unknown";
        s.preset = null;
        s.hudX = -30;
        s.hudY = 999999;
        s.normalize();
        assertEquals("BLUE", s.theme);
        assertEquals("BALANCED", s.preset);
        assertEquals(0, s.hudX);
        assertEquals(10000, s.hudY);
    }

    @Test
    void performancePresetRemovesTflAnimationNotHud() {
        TflSettings s = new TflSettings();
        s.applyPreset("FPS");
        assertFalse(s.animatedBackground);
        assertTrue(s.reducedMotion);
        assertTrue(s.hudEnabled);
        assertEquals("FPS", s.preset);
        s.applyPreset("QUALITY");
        assertTrue(s.animatedBackground);
        assertFalse(s.reducedMotion);
    }

    @Test
    void lowercaseKnownThemeIsSupported() {
        TflSettings s = new TflSettings();
        s.theme = "midnight";
        s.normalize();
        assertEquals("MIDNIGHT", s.theme);
    }
}
