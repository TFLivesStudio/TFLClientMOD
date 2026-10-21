package online.tflives.tflcore.client.config;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class SettingsStoreTest {
    @TempDir
    Path temp;

    private SettingsStore store(Path target, AtomicLong clock) {
        SettingsStore store = new SettingsStore(target, clock::get);
        store.setDebounceMillis(SettingsStore.DEFAULT_DEBOUNCE_MILLIS);
        return store;
    }

    @Test
    void roundTripPersistsSchemaTwoData() {
        Path target = temp.resolve("subdir/tflcore.json");
        AtomicLong clock = new AtomicLong(1_000);
        SettingsStore first = store(target, clock);
        first.get().ui.theme = "FROST";
        first.get().setModuleEnabled("zoom", false);
        WidgetPlacement placement = first.get().widget("fps", new WidgetPlacement());
        placement.x = 42;
        placement.anchor = WidgetPlacement.TOP_RIGHT;
        assertTrue(first.saveNow());

        SettingsStore second = store(target, clock);
        assertEquals("FROST", second.get().ui.theme);
        assertFalse(second.get().isModuleEnabled("zoom", true));
        assertEquals(42, second.get().widgetOrNull("fps").x);
        assertEquals(WidgetPlacement.TOP_RIGHT, second.get().widgetOrNull("fps").anchor);
        assertEquals(TflSettings.SCHEMA_VERSION, second.get().schemaVersion);
        assertFalse(second.get().telemetryConsent);
    }

    @Test
    void writesAreDebouncedAndFlushedOnTick() {
        Path target = temp.resolve("tflcore.json");
        AtomicLong clock = new AtomicLong(1_000);
        SettingsStore store = store(target, clock);
        store.get().ui.theme = "EMBER";
        store.requestSave();
        assertTrue(store.isDirty());

        assertFalse(store.tick(), "the write must not happen immediately");
        clock.addAndGet(200);
        assertFalse(store.tick(), "the debounce window is 350 ms");
        clock.addAndGet(200);
        assertTrue(store.tick());
        assertFalse(store.isDirty());
    }

    @Test
    void flushWritesImmediately() throws Exception {
        Path target = temp.resolve("tflcore.json");
        AtomicLong clock = new AtomicLong(1_000);
        SettingsStore store = store(target, clock);
        store.get().ui.theme = "MONO";
        store.requestSave();
        store.flush();
        assertTrue(Files.exists(target));
        assertTrue(Files.readString(target).contains("MONO"));
    }

    @Test
    void corruptJsonIsPreservedAndFlagged() throws Exception {
        Path target = temp.resolve("tflcore.json");
        String corrupt = "{ broken JSON";
        Files.writeString(target, corrupt, StandardCharsets.UTF_8);
        SettingsStore store = new SettingsStore(target);

        assertTrue(store.isCorrupt());
        assertTrue(store.get().ui.customMainMenu);
        assertEquals(corrupt, Files.readString(target), "the unreadable file stays untouched on load");
        assertEquals(1, store.backups().size(), "a corrupt file is backed up");
    }

    @Test
    void futureSchemaIsReadOnlyAndCannotBeOverwritten() throws Exception {
        Path target = temp.resolve("tflcore.json");
        String future = "{\"schemaVersion\":999,\"ui\":{\"theme\":\"FROST\"}}";
        Files.writeString(target, future, StandardCharsets.UTF_8);
        SettingsStore store = new SettingsStore(target);

        assertTrue(store.isReadOnly());
        assertEquals("BLUE", store.get().ui.theme);
        assertFalse(store.saveNow());
        assertFalse(store.tick());
        assertEquals(future, Files.readString(target));
    }

    @Test
    void migrationWritesSchemaTwoAndCreatesOneBackup() throws Exception {
        Path target = temp.resolve("tflcore.json");
        Files.writeString(target, """
                {"theme":"FROST","hudEnabled":false,"showFps":false}
                """, StandardCharsets.UTF_8);
        AtomicLong clock = new AtomicLong(10_000);
        SettingsStore store = store(target, clock);

        assertTrue(store.wasMigrated());
        assertEquals("FROST", store.get().ui.theme);
        assertFalse(store.get().hud.enabled);
        assertTrue(store.isDirty(), "migration must be persisted");
        assertTrue(store.tick(), "the migrated document is written on the next tick");

        String saved = Files.readString(target);
        assertTrue(saved.contains("\"schemaVersion\": 2"));
        assertTrue(saved.contains("FROST"));
        assertEquals(1, store.backups().size());

        SettingsStore reloaded = store(target, clock);
        assertFalse(reloaded.wasMigrated(), "migration must not repeat on every start");
        assertEquals(1, reloaded.backups().size());
    }

    @Test
    void exportAndImportStayInsideTheConfigDirectory() throws Exception {
        Path config = temp.resolve("config");
        Files.createDirectories(config);
        Path target = config.resolve("tflcore.json");
        AtomicLong clock = new AtomicLong(1_000);
        SettingsStore source = store(target, clock);
        source.get().ui.theme = "FROST";
        source.get().hud.globalScale = 1.25F;

        Path export = config.resolve("tflcore-export.json");
        assertEquals(SettingsStore.ImportResult.OK, source.exportTo(export));
        assertTrue(Files.exists(export));

        SettingsStore destination = store(config.resolve("second.json"), clock);
        assertEquals(SettingsStore.ImportResult.OK, destination.importFrom(export));
        assertEquals("FROST", destination.get().ui.theme);
        assertEquals(1.25F, destination.get().hud.globalScale, 0.0001F);

        Path outside = temp.resolve("outside.json");
        assertEquals(SettingsStore.ImportResult.UNSAFE_PATH, destination.importFrom(outside));
        assertEquals(SettingsStore.ImportResult.UNSAFE_PATH, destination.exportTo(outside));
        assertFalse(Files.exists(outside));
    }

    @Test
    void importRejectsMissingInvalidOversizedAndFutureFiles() throws Exception {
        Path config = temp.resolve("config");
        Files.createDirectories(config);
        SettingsStore store = store(config.resolve("tflcore.json"), new AtomicLong(1_000));

        assertEquals(SettingsStore.ImportResult.MISSING, store.importFrom(config.resolve("nope.json")));

        Path invalid = config.resolve("invalid.json");
        Files.writeString(invalid, "[]", StandardCharsets.UTF_8);
        assertEquals(SettingsStore.ImportResult.INVALID, store.importFrom(invalid));

        Path tooLarge = config.resolve("large.json");
        Files.write(tooLarge, new byte[(int) SettingsStore.MAX_FILE_BYTES + 1]);
        assertEquals(SettingsStore.ImportResult.TOO_LARGE, store.importFrom(tooLarge));

        Path future = config.resolve("future.json");
        Files.writeString(future, "{\"schemaVersion\":999}", StandardCharsets.UTF_8);
        assertEquals(SettingsStore.ImportResult.UNSUPPORTED_SCHEMA, store.importFrom(future));
    }

    @Test
    void importCreatesBackupOfCurrentFile() throws Exception {
        Path config = temp.resolve("config");
        Files.createDirectories(config);
        Path target = config.resolve("tflcore.json");
        SettingsStore store = store(target, new AtomicLong(1_000));
        store.saveNow();
        Path source = config.resolve("incoming.json");
        Files.writeString(source, "{\"schemaVersion\":2,\"ui\":{\"theme\":\"EMBER\"}}",
                StandardCharsets.UTF_8);

        assertEquals(SettingsStore.ImportResult.OK, store.importFrom(source));
        assertEquals("EMBER", store.get().ui.theme);
        assertTrue(store.backups().size() >= 1);
    }

    @Test
    void resetToDefaultsClearsUserStateAndWrites() throws Exception {
        Path target = temp.resolve("tflcore.json");
        SettingsStore store = store(target, new AtomicLong(1_000));
        store.get().ui.theme = "FROST";
        store.get().setModuleEnabled("zoom", false);
        assertTrue(store.resetToDefaults());
        assertEquals("BLUE", store.get().ui.theme);
        assertEquals(TflSettings.SCHEMA_VERSION, store.get().schemaVersion);
        assertTrue(store.get().modules.isEmpty());
        assertTrue(Files.readString(target).contains("\"schemaVersion\": 2"));
    }

    @Test
    void toJsonRoundTripsThroughAReadOnlyCopy() throws Exception {
        SettingsStore store = store(temp.resolve("tflcore.json"), new AtomicLong(1_000));
        store.get().ui.theme = "FROST";
        String json = store.toJson();
        Path copy = temp.resolve("copy.json");
        Files.writeString(copy, json, StandardCharsets.UTF_8);
        TflSettings read = SettingsStore.readOnlyCopy(copy);
        assertEquals("FROST", read.ui.theme);
        assertEquals(TflSettings.SCHEMA_VERSION, read.schemaVersion);
    }

    @Test
    void atomicWritesLeaveNoTemporaryFiles() throws Exception {
        Path target = temp.resolve("tflcore.json");
        SettingsStore store = store(target, new AtomicLong(1_000));
        store.get().ui.theme = "FROST";
        store.saveNow();
        try (Stream<Path> files = Files.list(temp)) {
            List<Path> leftovers = files.filter(path -> path.getFileName().toString().endsWith(".tmp")).toList();
            assertTrue(leftovers.isEmpty());
        }
        assertTrue(Files.exists(target));
        assertNotEquals(0L, Files.size(target));
    }
}
