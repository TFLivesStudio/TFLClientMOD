package online.tflives.tflcore.client.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

/** Safe and intentionally synchronous: writes happen only when preferences change. */
public final class SettingsStore {
    private static final Logger LOGGER = LoggerFactory.getLogger("tflcore");
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private final Path path;
    private TflSettings settings;
    private boolean readOnly;

    public SettingsStore(Path path) {
        this.path = path;
        this.settings = load();
    }

    public TflSettings get() {
        return settings;
    }

    private TflSettings load() {
        if (!Files.exists(path)) return new TflSettings();
        try (Reader reader = Files.newBufferedReader(path, StandardCharsets.UTF_8)) {
            TflSettings value = GSON.fromJson(reader, TflSettings.class);
            if (value == null) throw new IllegalArgumentException("Settings JSON is null");
            if (value.schemaVersion > TflSettings.SCHEMA_VERSION) {
                readOnly = true;
                LOGGER.warn("Settings schema {} is newer than this TFL Core. Using defaults; file is read-only.", value.schemaVersion);
                return new TflSettings();
            }
            value.normalize();
            return value;
        } catch (Exception e) {
            LOGGER.warn("Could not read {}. Using defaults; original file remains untouched.", path, e);
            return new TflSettings();
        }
    }

    public boolean save() {
        if (readOnly) {
            LOGGER.warn("Refusing to overwrite settings from a newer TFL Core version: {}", path);
            return false;
        }
        Path tmp = null;
        try {
            Files.createDirectories(path.toAbsolutePath().getParent());
            tmp = Files.createTempFile(path.toAbsolutePath().getParent(), "tflcore-", ".tmp");
            settings.normalize();
            try (Writer writer = Files.newBufferedWriter(tmp, StandardCharsets.UTF_8)) {
                GSON.toJson(settings, writer);
            }
            try {
                Files.move(tmp, path, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
            } catch (AtomicMoveNotSupportedException unsupported) {
                Files.move(tmp, path, StandardCopyOption.REPLACE_EXISTING);
            }
            return true;
        } catch (Exception e) {
            LOGGER.error("Could not save TFL Core settings to {}", path, e);
            return false;
        } finally {
            if (tmp != null) {
                try { Files.deleteIfExists(tmp); }
                catch (Exception ignored) { }
            }
        }
    }
}
