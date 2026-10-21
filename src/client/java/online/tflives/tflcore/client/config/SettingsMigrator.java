package online.tflives.tflcore.client.config;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import java.util.LinkedHashSet;
import java.util.Set;

/**
 * Explicit, lossless-until-saved migration between settings schemas.
 *
 * <p>Migration works on a {@link JsonObject} so it never relies on field reflection and can be
 * unit tested without touching disk. Unknown v1 keys are preserved under {@code legacy} instead of
 * being silently discarded.</p>
 */
public final class SettingsMigrator {
    private SettingsMigrator() {
    }

    public static final class Result {
        public final JsonObject json;
        public final int from;
        public final boolean migrated;

        Result(JsonObject json, int from, boolean migrated) {
            this.json = json;
            this.from = from;
            this.migrated = migrated;
        }
    }

    public static Result migrate(JsonObject input) {
        int from = readSchema(input);
        if (from > TflSettings.SCHEMA_VERSION) {
            throw new UnsupportedSchemaException(from);
        }
        if (from == TflSettings.SCHEMA_VERSION) {
            return new Result(input.deepCopy(), from, false);
        }
        JsonObject migrated = from == 1 ? migrateV1(input) : migrateUnknown(input);
        migrated.addProperty("schemaVersion", TflSettings.SCHEMA_VERSION);
        return new Result(migrated, from, true);
    }

    public static int readSchema(JsonObject input) {
        JsonElement element = input.get("schemaVersion");
        if (element == null || !element.isJsonPrimitive()) {
            return 1; // 0.1/0.2 files did not always persist a schema version
        }
        try {
            return element.getAsInt();
        } catch (RuntimeException ignored) {
            return 1;
        }
    }

    private static JsonObject migrateUnknown(JsonObject input) {
        JsonObject out = input.deepCopy();
        out.remove("schemaVersion");
        JsonObject wrapped = new JsonObject();
        wrapped.addProperty("schemaVersion", TflSettings.SCHEMA_VERSION);
        wrapped.add("legacy", out);
        return wrapped;
    }

    private static JsonObject migrateV1(JsonObject v1) {
        JsonObject ui = new JsonObject();
        JsonObject hud = new JsonObject();
        JsonObject pvp = new JsonObject();
        JsonObject survival = new JsonObject();
        JsonObject perf = new JsonObject();
        JsonObject modules = new JsonObject();
        JsonObject widgets = new JsonObject();

        ui.addProperty("theme", string(v1, "theme", "BLUE"));

        Set<String> consumed = new LinkedHashSet<>();

        boolean customMainMenu = bool(v1, "customMainMenu", true);
        boolean animatedBackground = bool(v1, "animatedBackground", true);
        boolean reducedMotion = bool(v1, "reducedMotion", false);
        ui.addProperty("customMainMenu", customMainMenu);
        ui.addProperty("animatedBackground", animatedBackground);
        ui.addProperty("reducedMotion", reducedMotion);
        consumed.add("customMainMenu");
        consumed.add("animatedBackground");
        consumed.add("reducedMotion");
        consumed.add("theme");

        boolean hudEnabled = bool(v1, "hudEnabled", true);
        boolean showFps = bool(v1, "showFps", true);
        boolean showCoordinates = bool(v1, "showCoordinates", true);
        boolean showClock = bool(v1, "showClock", false);
        int hudX = integer(v1, "hudX", 8);
        int hudY = integer(v1, "hudY", 8);
        hud.addProperty("enabled", hudEnabled);
        consumed.add("hudEnabled");
        consumed.add("hudX");
        consumed.add("hudY");

        int row = 0;
        widgets.add("fps", legacyWidget(showFps, hudX, hudY + 14 * row++));
        widgets.add("coordinates", legacyWidget(showCoordinates, hudX, hudY + 14 * row++));
        widgets.add("clock", legacyWidget(showClock, hudX, hudY + 14 * row));
        consumed.add("showFps");
        consumed.add("showCoordinates");
        consumed.add("showClock");
        hud.add("widgets", widgets);

        perf.addProperty("preset", string(v1, "preset", "BALANCED"));
        consumed.add("preset");

        pvp.addProperty("customCrosshair", false); // 0.2 did not replace the crosshair
        survival.addProperty("zoom", false);
        survival.addProperty("deathWaypoint", true);
        survival.add("waypoints", new com.google.gson.JsonArray());
        // 0.2 had no gameplay modules; keep its behaviour after an upgrade.
        modules.addProperty("zoom", false);
        modules.addProperty("toggle-sprint", false);
        modules.addProperty("waypoints", true);

        JsonObject out = new JsonObject();
        out.add("ui", ui);
        out.add("hud", hud);
        out.add("pvp", pvp);
        out.add("survival", survival);
        out.add("perf", perf);
        out.add("modules", modules);
        out.addProperty("telemetryConsent", bool(v1, "telemetryConsent", false));
        consumed.add("telemetryConsent");
        consumed.add("schemaVersion");

        JsonObject legacy = new JsonObject();
        for (String key : v1.keySet()) {
            if (!consumed.contains(key)) {
                legacy.add(key, v1.get(key));
            }
        }
        if (!legacy.isEmpty()) {
            out.add("legacy", legacy);
        }
        return out;
    }

    private static JsonObject legacyWidget(boolean enabled, int x, int y) {
        JsonObject widget = new JsonObject();
        widget.addProperty("enabled", enabled);
        widget.addProperty("anchor", WidgetPlacement.TOP_LEFT);
        widget.addProperty("x", Math.max(0, Math.min(x, 2048)));
        widget.addProperty("y", Math.max(0, Math.min(y, 2048)));
        widget.addProperty("scale", 1.0F);
        widget.addProperty("background", true);
        return widget;
    }

    private static String string(JsonObject json, String key, String fallback) {
        JsonElement element = json.get(key);
        if (element == null || !element.isJsonPrimitive()) return fallback;
        try {
            String value = element.getAsString();
            return value == null || value.isBlank() ? fallback : value;
        } catch (RuntimeException ignored) {
            return fallback;
        }
    }

    private static boolean bool(JsonObject json, String key, boolean fallback) {
        JsonElement element = json.get(key);
        if (element == null || !element.isJsonPrimitive()) return fallback;
        try {
            return element.getAsBoolean();
        } catch (RuntimeException ignored) {
            return fallback;
        }
    }

    private static int integer(JsonObject json, String key, int fallback) {
        JsonElement element = json.get(key);
        if (element == null || !element.isJsonPrimitive()) return fallback;
        try {
            return element.getAsInt();
        } catch (RuntimeException ignored) {
            return fallback;
        }
    }

    public static final class UnsupportedSchemaException extends RuntimeException {
        public UnsupportedSchemaException(int schema) {
            super("Unsupported settings schema: " + schema);
        }
    }
}
