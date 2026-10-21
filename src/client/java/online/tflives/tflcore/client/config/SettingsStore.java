package online.tflives.tflcore.client.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.function.LongSupplier;
import java.util.stream.Stream;

/**
 * Local-only settings persistence.
 *
 * <p>Writes are debounced (never per frame), atomic, and preceded by a backup when the schema is
 * migrated or a file is imported. Import/export paths are restricted to the Minecraft config
 * directory, file sizes are bounded, and a newer schema is never overwritten.</p>
 */
public final class SettingsStore {
    public static final long MAX_FILE_BYTES = 1L << 20; // 1 MiB
    public static final long DEFAULT_DEBOUNCE_MILLIS = 350L;
    private static final Logger LOGGER = LoggerFactory.getLogger("tflcore");
    private static final Gson GSON = new GsonBuilder()
            .setPrettyPrinting()
            .disableHtmlEscaping()
            .create();
    private static final DateTimeFormatter BACKUP_STAMP = DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss-SSS");
    private static final int MAX_BACKUPS = 12;
    private static final String BACKUP_DIR = "tflcore-backups";

    public enum ImportResult {
        OK, MISSING, TOO_LARGE, INVALID, UNSUPPORTED_SCHEMA, UNSAFE_PATH, IO_ERROR
    }

    private final Path path;
    private final Path configRoot;
    private final LongSupplier clock;
    private TflSettings settings;
    private boolean readOnly;
    private boolean corrupt;
    private boolean dirty;
    private boolean migrated;
    private long dirtyAt;
    private long revision;
    private long debounceMillis = DEFAULT_DEBOUNCE_MILLIS;

    public SettingsStore(Path path) {
        this(path, System::currentTimeMillis);
    }

    SettingsStore(Path path, LongSupplier clock) {
        this.path = path.toAbsolutePath().normalize();
        this.configRoot = this.path.getParent() == null
                ? Path.of(".").toAbsolutePath().normalize()
                : this.path.getParent();
        this.clock = clock;
        this.settings = load();
    }

    public TflSettings get() {
        return settings;
    }

    public Path path() {
        return path;
    }

    public Path configRoot() {
        return configRoot;
    }

    public Path backupDir() {
        return configRoot.resolve(BACKUP_DIR);
    }

    public boolean isReadOnly() {
        return readOnly;
    }

    public boolean isCorrupt() {
        return corrupt;
    }

    public boolean wasMigrated() {
        return migrated;
    }

    public boolean isDirty() {
        return dirty;
    }

    /** Increments whenever the in-memory state is replaced by a load/import/reset. */
    public long revision() {
        return revision;
    }

    public void setDebounceMillis(long debounceMillis) {
        this.debounceMillis = Math.max(0, debounceMillis);
    }

    private TflSettings load() {
        if (!Files.exists(path)) {
            return defaults();
        }
        try {
            long size = Files.size(path);
            if (size > MAX_FILE_BYTES) {
                readOnly = true;
                corrupt = true;
                LOGGER.error("Settings file {} is too large ({} bytes); using defaults in read-only mode.", path, size);
                return defaults();
            }
            String text = Files.readString(path, StandardCharsets.UTF_8);
            JsonElement parsed = JsonParser.parseString(text);
            if (!parsed.isJsonObject()) {
                throw new IllegalArgumentException("Settings root is not an object");
            }
            JsonObject root = parsed.getAsJsonObject();
            int schema = SettingsMigrator.readSchema(root);
            if (schema > TflSettings.SCHEMA_VERSION) {
                readOnly = true;
                LOGGER.warn("Settings schema {} is newer than this TFL Core ({}). Using defaults; file is read-only.",
                        schema, TflSettings.SCHEMA_VERSION);
                return defaults();
            }
            SettingsMigrator.Result result = SettingsMigrator.migrate(root);
            TflSettings value = GSON.fromJson(result.json, TflSettings.class);
            if (value == null) {
                throw new IllegalArgumentException("Settings JSON is null");
            }
            value.normalize();
            if (result.migrated) {
                migrated = true;
                // Persist the upgraded document on the next tick so migration is not repeated.
                dirty = true;
                dirtyAt = 0;
                writeBackup(text, "pre-migration");
                LOGGER.info("Migrated TFL Core settings from schema {} to {}. Original kept in {}.",
                        result.from, TflSettings.SCHEMA_VERSION, backupDir());
            }
            return value;
        } catch (SettingsMigrator.UnsupportedSchemaException unsupported) {
            readOnly = true;
            LOGGER.warn("{}; using defaults in read-only mode.", unsupported.getMessage());
            return defaults();
        } catch (Exception e) {
            corrupt = true;
            try {
                String text = Files.readString(path, StandardCharsets.UTF_8);
                writeBackup(text, "corrupt");
            } catch (Exception backupFailure) {
                LOGGER.warn("Could not back up unreadable settings file {}", path, backupFailure);
            }
            LOGGER.warn("Could not read {}. Using defaults; original file remains untouched.", path, e);
            return defaults();
        }
    }

    /** Marks the settings as changed; the actual disk write happens on {@link #tick()}. */
    public void requestSave() {
        dirty = true;
        dirtyAt = clock.getAsLong();
        if (debounceMillis == 0) {
            saveNow();
        }
    }

    /** Called once per client tick. Returns true when a deferred write was flushed. */
    public boolean tick() {
        if (!dirty || readOnly) {
            return false;
        }
        if (clock.getAsLong() - dirtyAt < debounceMillis) {
            return false;
        }
        return saveNow();
    }

    public void flush() {
        if (dirty && !readOnly) {
            saveNow();
        }
    }

    public boolean saveNow() {
        if (readOnly) {
            LOGGER.warn("Refusing to overwrite read-only TFL Core settings: {}", path);
            return false;
        }
        try {
            settings.normalize();
            writeJson(GSON.toJsonTree(settings));
            dirty = false;
            revision++;
            return true;
        } catch (Exception e) {
            LOGGER.error("Could not save TFL Core settings to {}", path, e);
            return false;
        }
    }

    public boolean resetToDefaults() {
        settings = defaults();
        revision++;
        dirty = true;
        return saveNow();
    }

    public ImportResult importFrom(Path source) {
        if (!isInsideConfig(source)) {
            return ImportResult.UNSAFE_PATH;
        }
        if (!Files.isRegularFile(source)) {
            return ImportResult.MISSING;
        }
        try {
            if (Files.size(source) > MAX_FILE_BYTES) {
                return ImportResult.TOO_LARGE;
            }
            String text = Files.readString(source, StandardCharsets.UTF_8);
            JsonElement parsed = JsonParser.parseString(text);
            if (!parsed.isJsonObject()) {
                return ImportResult.INVALID;
            }
            JsonObject root = parsed.getAsJsonObject();
            if (SettingsMigrator.readSchema(root) > TflSettings.SCHEMA_VERSION) {
                return ImportResult.UNSUPPORTED_SCHEMA;
            }
            SettingsMigrator.Result result = SettingsMigrator.migrate(root);
            TflSettings imported = GSON.fromJson(result.json, TflSettings.class);
            if (imported == null) {
                return ImportResult.INVALID;
            }
            imported.normalize();
            if (Files.exists(path)) {
                writeBackup(Files.readString(path, StandardCharsets.UTF_8), "pre-import");
            }
            settings = imported;
            readOnly = false;
            corrupt = false;
            revision++;
            dirty = true;
            saveNow();
            LOGGER.info("Imported TFL Core settings from {}", source.getFileName());
            return ImportResult.OK;
        } catch (SettingsMigrator.UnsupportedSchemaException unsupported) {
            return ImportResult.UNSUPPORTED_SCHEMA;
        } catch (Exception e) {
            LOGGER.warn("Rejected settings import from {}", source.getFileName(), e);
            return ImportResult.INVALID;
        }
    }

    public ImportResult exportTo(Path target) {
        if (!isInsideConfig(target)) {
            return ImportResult.UNSAFE_PATH;
        }
        try {
            settings.normalize();
            writeJsonTo(target, GSON.toJsonTree(settings));
            return ImportResult.OK;
        } catch (Exception e) {
            LOGGER.warn("Could not export TFL Core settings to {}", target, e);
            return ImportResult.IO_ERROR;
        }
    }

    public List<Path> backups() {
        Path dir = backupDir();
        if (!Files.isDirectory(dir)) {
            return List.of();
        }
        try (Stream<Path> stream = Files.list(dir)) {
            List<Path> result = new ArrayList<>(stream
                    .filter(Files::isRegularFile)
                    .sorted(Comparator.comparing((Path p) -> p.getFileName().toString()).reversed())
                    .limit(MAX_BACKUPS)
                    .toList());
            return result;
        } catch (IOException e) {
            LOGGER.warn("Could not list TFL Core backups", e);
            return List.of();
        }
    }

    private void writeJson(JsonElement json) throws IOException {
        writeJsonTo(path, json);
    }

    private void writeJsonTo(Path target, JsonElement json) throws IOException {
        Path absolute = target.toAbsolutePath().normalize();
        Path parent = absolute.getParent();
        if (parent == null) {
            throw new IOException("Target has no parent directory: " + target);
        }
        Files.createDirectories(parent);
        Path tmp = Files.createTempFile(parent, "tflcore-", ".tmp");
        try {
            try (Writer writer = Files.newBufferedWriter(tmp, StandardCharsets.UTF_8)) {
                GSON.toJson(json, writer);
            }
            try {
                Files.move(tmp, absolute, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
            } catch (AtomicMoveNotSupportedException unsupported) {
                Files.move(tmp, absolute, StandardCopyOption.REPLACE_EXISTING);
            }
        } finally {
            try {
                Files.deleteIfExists(tmp);
            } catch (IOException ignored) {
                // best effort
            }
        }
    }

    private void writeBackup(String text, String reason) {
        try {
            Path dir = backupDir();
            Files.createDirectories(dir);
            String stamp = LocalDateTime.now().format(BACKUP_STAMP);
            Path backup = dir.resolve("tflcore-" + stamp + "-" + reason + ".json");
            Files.writeString(backup, text, StandardCharsets.UTF_8);
            pruneBackups(dir);
        } catch (Exception e) {
            LOGGER.warn("Could not create a settings backup in {}", backupDir(), e);
        }
    }

    private void pruneBackups(Path dir) {
        try (Stream<Path> stream = Files.list(dir)) {
            List<Path> files = stream
                    .filter(p -> p.getFileName().toString().startsWith("tflcore-"))
                    .filter(Files::isRegularFile)
                    .sorted(Comparator.comparing((Path p) -> p.getFileName().toString()).reversed())
                    .toList();
            for (int i = MAX_BACKUPS; i < files.size(); i++) {
                Files.deleteIfExists(files.get(i));
            }
        } catch (IOException e) {
            LOGGER.warn("Could not prune settings backups", e);
        }
    }

    private boolean isInsideConfig(Path candidate) {
        Path normalized = candidate.toAbsolutePath().normalize();
        return normalized.startsWith(configRoot);
    }

    public static TflSettings defaults() {
        TflSettings value = new TflSettings();
        value.normalize();
        return value;
    }

    /** Utility used by the UI to keep a readable copy of the current JSON. */
    public String toJson() {
        settings.normalize();
        return GSON.toJson(settings);
    }

    /** Reads a settings JSON without mutating the store; never executes anything from the file. */
    public static TflSettings readOnlyCopy(Path source) throws IOException {
        try (Reader reader = Files.newBufferedReader(source, StandardCharsets.UTF_8)) {
            TflSettings value = GSON.fromJson(reader, TflSettings.class);
            if (value == null) {
                throw new IOException("Empty settings document");
            }
            value.normalize();
            return value;
        }
    }
}
