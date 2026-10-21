package online.tflives.tflcore.client.config;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Versioned, local-only configuration for TFL Core.
 *
 * <p>Schema v2 groups preferences into typed sections and stores per-widget layout. The store never
 * persists credentials, tokens, server addresses or telemetry. File access is limited to the
 * Fabric configuration directory.</p>
 */
public final class TflSettings {
    public static final int SCHEMA_VERSION = 2;
    public static final int MAX_WAYPOINTS = 256;
    public static final int MAX_WIDGETS = 128;

    public int schemaVersion = SCHEMA_VERSION;
    public Ui ui = new Ui();
    public Hud hud = new Hud();
    public Pvp pvp = new Pvp();
    public Survival survival = new Survival();
    public Perf perf = new Perf();
    /** Module id -> enabled. Missing ids use the module default. */
    public Map<String, Boolean> modules = new LinkedHashMap<>();
    /** Kept for forward compatibility; TFL Core never transmits data. */
    public boolean telemetryConsent = false;
    /** Unknown keys preserved from older files so an upgrade never destroys user data. */
    public com.google.gson.JsonObject legacy;

    public static final class Ui {
        public String theme = "BLUE";
        public String accent = "DEFAULT";
        public boolean customMainMenu = true;
        public boolean animatedBackground = true;
        public boolean reducedMotion = false;
        public boolean highContrast = false;
        public int transitionMillis = 160;
        public boolean reducedTransparency = false;
        /** One-time in-game tips; resetting settings shows them again. */
        public boolean tipsShown = false;
    }

    public static final class Hud {
        public boolean enabled = true;
        public boolean snapToGrid = true;
        public int gridSize = 6;
        public float globalScale = 1.0F;
        public Map<String, WidgetPlacement> widgets = new LinkedHashMap<>();
        /** Replaces coordinates and ping with a neutral marker while streaming or recording. */
        public boolean streamerMode = false;
        /** Hides TFL widgets while any GUI screen is open. */
        public boolean hideInScreens = false;
    }

    public static final class Pvp {
        public boolean customCrosshair = true;
        public String crosshairStyle = "CROSS";
        public int crosshairColor = 0xFFFFFFFF;
        public float crosshairScale = 1.0F;
        public boolean crosshairHideThirdPerson = true;
        public boolean toggleSprint = false;
        public int hitColor = 0x66FF4D4D;
        public boolean hitFlash = true;
    }

    public static final class Survival {
        public boolean zoom = true;
        public int zoomFov = 25;
        public boolean smoothZoom = true;
        public boolean deathWaypoint = true;
        public List<Waypoint> waypoints = new ArrayList<>();
        public int activeWaypoint = 0;
    }

    public static final class Perf {
        public String preset = "BALANCED";
        public boolean diagnostics = true;
        public int benchmarkSeconds = 10;
    }

    public void normalize() {
        schemaVersion = SCHEMA_VERSION;
        // 0.3 never transmits data; an old opt-in flag must not enable anything in the future.
        telemetryConsent = false;
        if (ui == null) ui = new Ui();
        if (hud == null) hud = new Hud();
        if (pvp == null) pvp = new Pvp();
        if (survival == null) survival = new Survival();
        if (perf == null) perf = new Perf();
        if (modules == null) modules = new LinkedHashMap<>();
        if (hud.widgets == null) hud.widgets = new LinkedHashMap<>();
        if (survival.waypoints == null) survival.waypoints = new ArrayList<>();

        ui.theme = normalizeEnum(ui.theme, "BLUE", "BLUE", "MIDNIGHT", "FROST", "EMBER", "MONO");
        ui.accent = normalizeEnum(ui.accent, "DEFAULT", "DEFAULT", "CYAN", "VIOLET", "AMBER", "LIME", "ROSE");
        ui.transitionMillis = clamp(ui.transitionMillis, 0, 2000);

        hud.gridSize = clamp(hud.gridSize, 2, 32);
        if (!Float.isFinite(hud.globalScale)) hud.globalScale = 1.0F;
        hud.globalScale = Math.max(0.5F, Math.min(hud.globalScale, 2.5F));

        pvp.crosshairStyle = normalizeEnum(pvp.crosshairStyle, "CROSS", "CROSS", "DOT", "T", "CIRCLE", "SQUARE");
        if (!Float.isFinite(pvp.crosshairScale)) pvp.crosshairScale = 1.0F;
        pvp.crosshairScale = Math.max(0.5F, Math.min(pvp.crosshairScale, 3.0F));
        pvp.crosshairColor |= 0xFF000000;
        pvp.hitColor |= 0xFF000000;

        survival.zoomFov = clamp(survival.zoomFov, 5, 90);
        survival.activeWaypoint = Math.max(0, survival.activeWaypoint);
        normalizeWaypoints();
        normalizeWidgets();

        perf.preset = normalizeEnum(perf.preset, "BALANCED", "BALANCED", "FPS", "QUALITY", "CUSTOM");
        perf.benchmarkSeconds = clamp(perf.benchmarkSeconds, 3, 120);

        // Only keep overrides whose value is a plain boolean; ids stay stable strings.
        Map<String, Boolean> cleaned = new LinkedHashMap<>();
        int count = 0;
        for (Map.Entry<String, Boolean> entry : modules.entrySet()) {
            if (entry.getKey() == null || entry.getKey().isBlank() || entry.getValue() == null) continue;
            if (count++ >= MAX_WIDGETS) break;
            cleaned.put(entry.getKey(), entry.getValue());
        }
        modules = cleaned;
    }

    private void normalizeWaypoints() {
        List<Waypoint> cleaned = new ArrayList<>();
        for (Waypoint waypoint : survival.waypoints) {
            if (waypoint == null) continue;
            waypoint.normalize();
            if (cleaned.size() >= MAX_WAYPOINTS) break;
            cleaned.add(waypoint);
        }
        survival.waypoints = cleaned;
        if (survival.activeWaypoint >= cleaned.size()) {
            survival.activeWaypoint = 0;
        }
    }

    private void normalizeWidgets() {
        Map<String, WidgetPlacement> cleaned = new LinkedHashMap<>();
        int count = 0;
        for (Map.Entry<String, WidgetPlacement> entry : hud.widgets.entrySet()) {
            if (entry.getKey() == null || entry.getKey().isBlank() || entry.getValue() == null) continue;
            if (count++ >= MAX_WIDGETS) break;
            entry.getValue().normalize();
            cleaned.put(entry.getKey(), entry.getValue());
        }
        hud.widgets = cleaned;
    }

    /** Returns the stored placement or installs (and returns) a copy of the provided default. */
    public WidgetPlacement widget(String id, WidgetPlacement defaults) {
        WidgetPlacement placement = hud.widgets.get(id);
        if (placement == null) {
            placement = defaults.copy();
            hud.widgets.put(id, placement);
        }
        return placement;
    }

    public WidgetPlacement widgetOrNull(String id) {
        return hud.widgets.get(id);
    }

    public boolean isModuleEnabled(String id, boolean defaultValue) {
        Boolean override = modules.get(id);
        return override == null ? defaultValue : override;
    }

    public void setModuleEnabled(String id, boolean enabled) {
        modules.put(id, enabled);
    }

    public List<Waypoint> waypointsFor(String world, String dimension) {
        List<Waypoint> result = new ArrayList<>();
        for (Waypoint waypoint : survival.waypoints) {
            if (same(waypoint.world, world) && same(waypoint.dimension, dimension)) {
                result.add(waypoint);
            }
        }
        return result;
    }

    private static boolean same(String a, String b) {
        return a == null ? b == null || b.isEmpty() : a.equals(b);
    }

    public void applyPreset(String next) {
        String value = normalizeEnum(next, "BALANCED", "BALANCED", "FPS", "QUALITY", "CUSTOM");
        switch (value) {
            case "FPS" -> {
                perf.preset = "FPS";
                ui.animatedBackground = false;
                ui.reducedMotion = true;
                ui.reducedTransparency = true;
                hud.enabled = true;
            }
            case "QUALITY" -> {
                perf.preset = "QUALITY";
                ui.animatedBackground = true;
                ui.reducedMotion = false;
                ui.reducedTransparency = false;
                hud.enabled = true;
            }
            case "CUSTOM" -> perf.preset = "CUSTOM";
            default -> {
                perf.preset = "BALANCED";
                ui.animatedBackground = true;
                ui.reducedMotion = false;
                ui.reducedTransparency = false;
                hud.enabled = true;
            }
        }
    }

    /** Marks the settings as manually edited without touching the visual values. */
    public void markCustom() {
        perf.preset = "CUSTOM";
    }

    private static String normalizeEnum(String value, String fallback, String... options) {
        if (value == null) return fallback;
        for (String option : options) {
            if (option.equalsIgnoreCase(value)) return option;
        }
        return fallback;
    }

    private static int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(value, max));
    }

    public String summary() {
        return String.format(Locale.ROOT,
                "schema=%d theme=%s preset=%s modules=%d widgets=%d waypoints=%d",
                schemaVersion, ui.theme, perf.preset, modules.size(), hud.widgets.size(),
                survival.waypoints.size());
    }
}
