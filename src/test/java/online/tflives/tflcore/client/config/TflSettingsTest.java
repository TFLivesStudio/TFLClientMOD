package online.tflives.tflcore.client.config;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class TflSettingsTest {
    @Test
    void defaultsAreSchemaTwoAndConservative() {
        TflSettings settings = new TflSettings();
        settings.normalize();
        assertEquals(TflSettings.SCHEMA_VERSION, settings.schemaVersion);
        assertFalse(settings.telemetryConsent);
        assertTrue(settings.ui.customMainMenu);
        assertTrue(settings.ui.animatedBackground);
        assertFalse(settings.ui.reducedMotion);
        assertEquals("BLUE", settings.ui.theme);
        assertEquals(BALANCED, settings.perf.preset);
        assertTrue(settings.hud.enabled);
        assertFalse(settings.modules.containsKey("hud"));
        assertEquals(0xFFFFFFFF, settings.pvp.crosshairColor);
    }

    private static final String BALANCED = "BALANCED";

    @Test
    void telemetryConsentIsAlwaysDisabled() {
        TflSettings settings = new TflSettings();
        settings.telemetryConsent = true;
        settings.normalize();
        assertFalse(settings.telemetryConsent);
    }

    @Test
    void invalidValuesAreNormalized() {
        TflSettings settings = new TflSettings();
        settings.ui.theme = "unknown";
        settings.ui.accent = "also-unknown";
        settings.ui.transitionMillis = 99_999;
        settings.hud.gridSize = -5;
        settings.hud.globalScale = Float.NaN;
        settings.pvp.crosshairStyle = null;
        settings.pvp.crosshairScale = 99F;
        settings.pvp.crosshairColor = 0x00112233;
        settings.survival.zoomFov = 900;
        settings.perf.preset = "???";
        settings.perf.benchmarkSeconds = 1;
        settings.normalize();

        assertEquals("BLUE", settings.ui.theme);
        assertEquals("DEFAULT", settings.ui.accent);
        assertEquals(2000, settings.ui.transitionMillis);
        assertEquals(2, settings.hud.gridSize);
        assertEquals(1.0F, settings.hud.globalScale, 0.0001F);
        assertEquals("CROSS", settings.pvp.crosshairStyle);
        assertEquals(3.0F, settings.pvp.crosshairScale, 0.0001F);
        assertEquals(0xFF112233, settings.pvp.crosshairColor);
        assertEquals(90, settings.survival.zoomFov);
        assertEquals(BALANCED, settings.perf.preset);
        assertEquals(3, settings.perf.benchmarkSeconds);
    }

    @Test
    void missingSectionsAreRecreated() {
        TflSettings settings = new TflSettings();
        settings.ui = null;
        settings.hud = null;
        settings.pvp = null;
        settings.survival = null;
        settings.perf = null;
        settings.modules = null;
        settings.normalize();
        assertNotNull(settings.ui);
        assertNotNull(settings.hud);
        assertNotNull(settings.pvp);
        assertNotNull(settings.survival);
        assertNotNull(settings.perf);
        assertNotNull(settings.modules);
    }

    @Test
    void widgetPlacementsAreInstalledAndClamped() {
        TflSettings settings = new TflSettings();
        WidgetPlacement defaults = new WidgetPlacement(true, WidgetPlacement.BOTTOM_RIGHT, 6, 6, 1F, true);
        WidgetPlacement installed = settings.widget("fps", defaults);
        assertSame(installed, settings.widgetOrNull("fps"));
        installed.anchor = "NOT_AN_ANCHOR";
        installed.x = 99_999;
        installed.y = -99_999;
        installed.scale = 50F;
        installed.opacity = 0F;
        installed.normalize();
        assertEquals(WidgetPlacement.TOP_LEFT, installed.anchor);
        assertEquals(2048, installed.x);
        assertEquals(-2048, installed.y);
        assertEquals(3.0F, installed.scale, 0.0001F);
        assertEquals(0.15F, installed.opacity, 0.0001F);
        assertEquals(WidgetPlacement.BOTTOM_RIGHT, defaults.anchor,
                "the default placement must not be mutated by the stored copy");
    }

    @Test
    void widgetMapDropsNullsAndNormalizesEntries() {
        TflSettings settings = new TflSettings();
        settings.hud.widgets.put("good", new WidgetPlacement());
        settings.hud.widgets.put("bad", null);
        settings.hud.widgets.put(null, new WidgetPlacement());
        settings.normalize();
        assertEquals(1, settings.hud.widgets.size());
        assertNotNull(settings.hud.widgets.get("good"));
    }

    @Test
    void waypointsAreBoundedAndFiltered() {
        TflSettings settings = new TflSettings();
        for (int i = 0; i < 300; i++) {
            Waypoint waypoint = new Waypoint("w" + i, i % 2 == 0 ? "world" : "other",
                    i % 3 == 0 ? "minecraft:overworld" : "minecraft:the_nether",
                    1, 2, 3);
            settings.survival.waypoints.add(waypoint);
        }
        settings.survival.waypoints.add(null);
        settings.survival.activeWaypoint = 9999;
        settings.normalize();
        assertEquals(TflSettings.MAX_WAYPOINTS, settings.survival.waypoints.size());
        assertEquals(0, settings.survival.activeWaypoint);

        List<Waypoint> filtered = settings.waypointsFor("world", "minecraft:overworld");
        assertFalse(filtered.isEmpty());
        for (Waypoint waypoint : filtered) {
            assertEquals("world", waypoint.world);
            assertEquals("minecraft:overworld", waypoint.dimension);
        }
    }

    @Test
    void longWaypointNamesAreTruncated() {
        TflSettings settings = new TflSettings();
        Waypoint waypoint = new Waypoint("x".repeat(500), "w", "d", 0, 0, 0);
        settings.survival.waypoints.add(waypoint);
        settings.normalize();
        assertEquals(Waypoint.MAX_NAME_LENGTH, settings.survival.waypoints.get(0).name.length());
    }

    @Test
    void moduleOverridesRejectNullsAndPersistBooleans() {
        TflSettings settings = new TflSettings();
        settings.modules.put("zoom", false);
        settings.modules.put("toggle-sprint", true);
        settings.modules.put(null, true);
        settings.modules.put("bad", null);
        settings.normalize();
        assertEquals(2, settings.modules.size());
        assertFalse(settings.isModuleEnabled("zoom", true));
        assertTrue(settings.isModuleEnabled("toggle-sprint", false));
        assertTrue(settings.isModuleEnabled("missing", true));
        settings.setModuleEnabled("crosshair", false);
        assertFalse(settings.modules.get("crosshair"));
    }

    @Test
    void presetsOnlyTouchOwnedVisualSettings() {
        TflSettings settings = new TflSettings();
        settings.hud.enabled = false;
        settings.applyPreset("FPS");
        assertEquals("FPS", settings.perf.preset);
        assertFalse(settings.ui.animatedBackground);
        assertTrue(settings.ui.reducedMotion);
        assertTrue(settings.hud.enabled);

        settings.applyPreset("QUALITY");
        assertEquals("QUALITY", settings.perf.preset);
        assertTrue(settings.ui.animatedBackground);
        assertFalse(settings.ui.reducedMotion);

        settings.applyPreset("CUSTOM");
        assertEquals("CUSTOM", settings.perf.preset);
    }

    @Test
    void markCustomOnlyChangesThePreset() {
        TflSettings settings = new TflSettings();
        settings.ui.theme = "FROST";
        settings.markCustom();
        assertEquals("CUSTOM", settings.perf.preset);
        assertEquals("FROST", settings.ui.theme);
    }

    @Test
    void summaryIsStableAndContainsNoSecrets() {
        TflSettings settings = new TflSettings();
        settings.normalize();
        String summary = settings.summary();
        assertTrue(summary.contains("schema=2"));
        assertTrue(summary.contains("theme=BLUE"));
        assertFalse(summary.contains("token"));
        assertFalse(summary.contains("password"));
    }

    @Test
    void copyConstructorKeepsEveryPlacementField() {
        WidgetPlacement original = new WidgetPlacement(true, WidgetPlacement.CENTER, 12, 34, 1.5F, false);
        original.compact = true;
        original.opacity = 0.5F;
        WidgetPlacement copy = original.copy();
        assertEquals(original.anchor, copy.anchor);
        assertEquals(original.x, copy.x);
        assertEquals(original.y, copy.y);
        assertEquals(original.scale, copy.scale, 0.0001F);
        assertEquals(original.background, copy.background);
        assertEquals(original.compact, copy.compact);
        assertEquals(original.opacity, copy.opacity, 0.0001F);
        List<WidgetPlacement> list = new ArrayList<>();
        list.add(copy);
        assertEquals(1, list.size());
    }
}
