package online.tflives.tflcore.client.config;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class SettingsMigratorTest {
    @Test
    void migratesVersionOneFieldsIntoNestedSections() {
        JsonObject v1 = JsonParser.parseString("""
                {
                  "theme": "FROST",
                  "customMainMenu": false,
                  "animatedBackground": false,
                  "reducedMotion": true,
                  "hudEnabled": false,
                  "showFps": false,
                  "showCoordinates": true,
                  "showClock": true,
                  "hudX": 12,
                  "hudY": 40,
                  "preset": "FPS",
                  "telemetryConsent": false,
                  "futureKey": {"nested": [1, 2, 3]}
                }
                """).getAsJsonObject();

        SettingsMigrator.Result result = SettingsMigrator.migrate(v1);
        assertTrue(result.migrated);
        assertEquals(1, result.from);
        JsonObject out = result.json;
        assertEquals(TflSettings.SCHEMA_VERSION, out.get("schemaVersion").getAsInt());
        assertEquals("FROST", out.getAsJsonObject("ui").get("theme").getAsString());
        assertFalse(out.getAsJsonObject("ui").get("customMainMenu").getAsBoolean());
        assertFalse(out.getAsJsonObject("ui").get("animatedBackground").getAsBoolean());
        assertTrue(out.getAsJsonObject("ui").get("reducedMotion").getAsBoolean());
        assertFalse(out.getAsJsonObject("hud").get("enabled").getAsBoolean());

        JsonObject widgets = out.getAsJsonObject("hud").getAsJsonObject("widgets");
        assertFalse(widgets.getAsJsonObject("fps").get("enabled").getAsBoolean());
        assertTrue(widgets.getAsJsonObject("coordinates").get("enabled").getAsBoolean());
        assertTrue(widgets.getAsJsonObject("clock").get("enabled").getAsBoolean());
        assertEquals(12, widgets.getAsJsonObject("fps").get("x").getAsInt());
        assertEquals(40, widgets.getAsJsonObject("fps").get("y").getAsInt());

        assertEquals("FPS", out.getAsJsonObject("perf").get("preset").getAsString());
        JsonObject modules = out.getAsJsonObject("modules");
        assertFalse(modules.get("zoom").getAsBoolean());
        assertFalse(modules.get("toggle-sprint").getAsBoolean());
        assertTrue(modules.get("waypoints").getAsBoolean());
        assertFalse(modules.has("hud"), "the old HUD pseudo-module must not survive migration");

        JsonObject legacy = out.getAsJsonObject("legacy");
        assertNotNull(legacy);
        assertEquals(1, legacy.getAsJsonObject("futureKey")
                .getAsJsonArray("nested").get(0).getAsInt());
    }

    @Test
    void versionTwoIsPassedThroughWithoutChanges() {
        JsonObject v2 = JsonParser.parseString("""
                {"schemaVersion":2,"ui":{"theme":"EMBER"},"modules":{"zoom":false}}
                """).getAsJsonObject();
        SettingsMigrator.Result result = SettingsMigrator.migrate(v2);
        assertFalse(result.migrated);
        assertEquals(2, result.from);
        assertEquals(v2.toString(), result.json.toString());
        assertFalse(result.json == v2, "migration must not hand out the caller's mutable object");
    }

    @Test
    void missingSchemaIsTreatedAsVersionOne() {
        JsonObject legacy = JsonParser.parseString("""
                {"theme":"MIDNIGHT","showFps":false}
                """).getAsJsonObject();
        assertEquals(1, SettingsMigrator.readSchema(legacy));
        SettingsMigrator.Result result = SettingsMigrator.migrate(legacy);
        assertTrue(result.migrated);
        assertEquals("MIDNIGHT", result.json.getAsJsonObject("ui").get("theme").getAsString());
    }

    @Test
    void unknownOlderSchemaIsWrappedInsteadOfDiscarded() {
        JsonObject old = JsonParser.parseString("""
                {"schemaVersion":0,"something":"kept"}
                """).getAsJsonObject();
        SettingsMigrator.Result result = SettingsMigrator.migrate(old);
        assertTrue(result.migrated);
        assertEquals(0, result.from);
        assertEquals("kept", result.json.getAsJsonObject("legacy").get("something").getAsString());
    }

    @Test
    void futureSchemaIsRejected() {
        JsonObject future = JsonParser.parseString("""
                {"schemaVersion":999}
                """).getAsJsonObject();
        assertThrows(SettingsMigrator.UnsupportedSchemaException.class,
                () -> SettingsMigrator.migrate(future));
    }

    @Test
    void nonNumericSchemaFallsBackToOne() {
        JsonObject broken = JsonParser.parseString("""
                {"schemaVersion":"not-a-number","theme":"FROST"}
                """).getAsJsonObject();
        assertEquals(1, SettingsMigrator.readSchema(broken));
    }
}
