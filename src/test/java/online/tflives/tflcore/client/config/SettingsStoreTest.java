package online.tflives.tflcore.client.config;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.charset.StandardCharsets;
import static org.junit.jupiter.api.Assertions.*;

final class SettingsStoreTest {
    @TempDir Path temp;

    @Test
    void roundTripPersistsIndependentModuleToggles() {
        Path target = temp.resolve("subdir/tflcore.json");
        SettingsStore first = new SettingsStore(target);
        first.get().showFps = false;
        first.get().hudY = 120;
        first.get().theme = "FROST";
        assertTrue(first.save());
        SettingsStore second = new SettingsStore(target);
        assertFalse(second.get().showFps);
        assertEquals(120, second.get().hudY);
        assertEquals("FROST", second.get().theme);
        assertFalse(second.get().telemetryConsent);
    }

    @Test
    void corruptJsonIsKeptAndDefaultsUsed() throws Exception {
        Path target = temp.resolve("tflcore.json");
        Files.writeString(target, "{ broken JSON", StandardCharsets.UTF_8);
        SettingsStore store = new SettingsStore(target);
        assertTrue(store.get().customMainMenu);
        assertEquals("{ broken JSON", Files.readString(target));
    }

    @Test
    void futureSchemaCannotBeOverwritten() throws Exception {
        Path target = temp.resolve("tflcore.json");
        String future = "{\"schemaVersion\":999,\"theme\":\"FROST\"}";
        Files.writeString(target, future);
        SettingsStore store = new SettingsStore(target);
        assertEquals("BLUE", store.get().theme);
        assertFalse(store.save());
        assertEquals(future, Files.readString(target));
    }
}
