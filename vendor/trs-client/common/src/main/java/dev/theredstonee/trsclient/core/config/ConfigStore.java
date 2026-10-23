package dev.theredstonee.trsclient.core.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonParseException;
import dev.theredstonee.trsclient.core.module.ModuleRegistry;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

/**
 * Lädt/speichert {@code trsclient.json}. Fehlt die Datei, gelten die Standardwerte;
 * ist sie kaputt, wird sie nach {@code trsclient.json.broken} verschoben und neu angelegt.
 */
public final class ConfigStore {
	/** Ergebnis des Ladens. */
	public enum Status {
		/** Datei gelesen. */
		LOADED,
		/** Unsupported future data or failed import: never write this file. */
		READ_ONLY,
		/** Keine Datei vorhanden – Standardwerte. */
		CREATED,
		/** Datei war unlesbar/kaputt – Standardwerte, Original gesichert. */
		RECOVERED
	}

	private static final Gson GSON = new GsonBuilder().serializeNulls().setPrettyPrinting().disableHtmlEscaping().create();

	private final Path file;
    private boolean readOnly;
    private String loadProblem;
    private com.google.gson.JsonObject preserved = new com.google.gson.JsonObject();
    public boolean isReadOnly() { return readOnly; }
    public String loadProblem() { return loadProblem; }

	/** Zuletzt geladene Config des Spiels (für Dienste ohne direkten Zugriff, z. B. den Client-Sync). */
	private static volatile ConfigStore active;
	private volatile Listener listener;

	/** Wird bei jedem Speichern mit dem festgehaltenen Stand aufgerufen (im Thread des Aufrufers). */
	public interface Listener {
		void saved(TrsConfig snapshot);
	}

	public ConfigStore(Path file) {
		this.file = file;
	}

	/** Die Config, die das Spiel zuletzt geladen hat (null vor dem Laden). */
	public static ConfigStore active() {
		return active;
	}

	/** Beobachter für Änderungen (ein einziger; null = keiner). */
	public void setListener(Listener listener) {
		this.listener = listener;
	}

	private void notifySaved(TrsConfig snapshot) {
		Listener l = listener;
		if (l == null) return;
		try {
			l.saved(snapshot);
		} catch (RuntimeException ignored) {
			// Ein Beobachter darf das Speichern nie stören.
		}
	}

	public Path file() {
		return file;
	}

	/** Pfad der Sicherung einer kaputten Datei. */
	public Path brokenFile() {
		return file.resolveSibling(file.getFileName() + ".broken");
	}

	/** Lädt die Config in die Module und legt sie bei Bedarf (neu) an. */
	public Status load(ModuleRegistry registry) {
		Status status;
        try { TflMigration.prepare(file); }
        catch (IOException | RuntimeException e) {
            readOnly = true; loadProblem = "Import blocked: " + e.getMessage();
            registry.apply(new TrsConfig()); active = this; return Status.READ_ONLY;
        }
        TrsConfig config = null;
		if (!Files.exists(file)) {
			status = Status.CREATED;
		} else {
			try (Reader reader = Files.newBufferedReader(file, StandardCharsets.UTF_8)) {
				if (Files.size(file) > TflMigration.MAX_BYTES) {
                    readOnly = true; loadProblem = "Configuration exceeds 1 MiB";
                    registry.apply(new TrsConfig()); active = this; return Status.READ_ONLY;
                }
                com.google.gson.JsonElement tree = new com.google.gson.JsonParser().parse(reader);
                if (!tree.isJsonObject()) throw new JsonParseException("Expected object");
                preserved = tree.getAsJsonObject();
                if (preserved.has("configVersion") && preserved.get("configVersion").getAsDouble() > TrsConfig.CURRENT_VERSION) {
                    readOnly = true; loadProblem = "Future configuration version";
                    registry.apply(new TrsConfig()); active = this; return Status.READ_ONLY;
                }
                if (preserved.has("modules") && !preserved.get("modules").isJsonNull() && !preserved.get("modules").isJsonObject()) throw new JsonParseException("Module config must be an object");
                config = GSON.fromJson(tree, TrsConfig.class);
				status = config == null ? Status.RECOVERED : Status.LOADED;
			} catch (JsonParseException | IllegalStateException | ClassCastException | IOException e) {
				status = Status.RECOVERED;
				config = null;
			}
			if (status == Status.RECOVERED) {
                preserved = new com.google.gson.JsonObject();
                if (!backupBroken()) { readOnly = true; loadProblem = "Cannot back up corrupt configuration"; }
            }
		}
		registry.apply(config != null ? config : new TrsConfig());
		active = this;
		if (status != Status.LOADED && !readOnly) {
			try {
				save(registry);
			} catch (IOException ignored) {
				// Speichern wird beim nächsten Schließen erneut versucht.
			}
		}
		return status;
	}

	/** Schreibt den aktuellen Zustand sofort und atomar (erst temporäre Datei, dann verschieben). */
	public void save(ModuleRegistry registry) throws IOException {
		TrsConfig snapshot = registry.capture();
		write(snapshot, sequence.incrementAndGet());
		notifySaved(snapshot);
	}

	/**
	 * Speichert im Hintergrund: der Zustand wird hier (im Spiel-Thread) festgehalten, geschrieben wird im Thread
	 * „TRS-Config“ – ein Tastendruck oder Menüklick wartet nie auf die Festplatte. Mehrere Aufrufe kurz
	 * hintereinander schreiben nur den neuesten Stand; beim Beenden des Spiels wird Ausstehendes noch geschrieben.
	 *
	 * @throws IOException der Fehler eines früheren Schreibens im Hintergrund (zum Loggen beim Aufrufer)
	 */
	public void saveLater(ModuleRegistry registry) throws IOException {
		TrsConfig snapshot = registry.capture();
		synchronized (pendingLock) {
			pending = snapshot;
			pendingSeq = sequence.incrementAndGet();
		}
		if (!scheduled.getAndSet(true)) worker().execute(this::flushPending);
		notifySaved(snapshot);
		IOException e = lastError;
		if (e != null) {
			lastError = null;
			throw e;
		}
	}

	/** Schreibt einen noch ausstehenden Stand sofort (z. B. beim Beenden) und wartet auf ein laufendes Schreiben. */
	public void flush() {
		flushPending();
		synchronized (writeLock) {
			// Ein gerade im Hintergrund laufendes Schreiben ist damit abgeschlossen.
		}
	}

	private final java.util.concurrent.atomic.AtomicLong sequence = new java.util.concurrent.atomic.AtomicLong();
	private final java.util.concurrent.atomic.AtomicBoolean scheduled = new java.util.concurrent.atomic.AtomicBoolean();
	private final Object pendingLock = new Object();
	private final Object writeLock = new Object();
	private TrsConfig pending;
	private long pendingSeq;
	private long writtenSeq;
	private volatile IOException lastError;
	private java.util.concurrent.ExecutorService worker;

	private synchronized java.util.concurrent.ExecutorService worker() {
		if (worker == null) {
			worker = java.util.concurrent.Executors.newSingleThreadExecutor(r -> {
				Thread t = new Thread(r, "TRS-Config");
				t.setDaemon(true);
				return t;
			});
			// Beim Beenden (System.exit) noch Ausstehendes schreiben.
			try {
				Runtime.getRuntime().addShutdownHook(new Thread(this::flushPending, "TRS-Config-Exit"));
			} catch (IllegalStateException | SecurityException ignored) {
				// JVM fährt schon herunter – dann schreibt der Aufrufer selbst.
			}
		}
		return worker;
	}

	private void flushPending() {
		scheduled.set(false);
		TrsConfig snapshot;
		long seq;
		synchronized (pendingLock) {
			snapshot = pending;
			seq = pendingSeq;
			pending = null;
		}
		if (snapshot == null) return;
		// Windows: solange jemand die Datei gerade liest (Virenscanner, Launcher), schlägt das Ersetzen fehl –
		// dann kurz warten und erneut versuchen.
		for (int attempt = 0; ; attempt++) {
			try {
				write(snapshot, seq);
				return;
			} catch (IOException e) {
				if (attempt >= 9) {
					lastError = e;
					return;
				}
			}
			try {
				Thread.sleep(25L * (attempt + 1));
			} catch (InterruptedException e) {
				Thread.currentThread().interrupt();
				return;
			}
		}
	}

	/** Schreibt {@code config}, außer ein neuerer Stand ist schon geschrieben. */
	private void write(TrsConfig config, long seq) throws IOException {
        if (readOnly) throw new IOException(loadProblem == null ? "Read-only configuration" : loadProblem);
		synchronized (writeLock) {
			if (seq <= writtenSeq) return;
			Path dir = file.toAbsolutePath().getParent();
			if (dir != null) Files.createDirectories(dir);
			String encoded = GSON.toJson(merge(preserved, GSON.toJsonTree(config).getAsJsonObject()));
            if (encoded.getBytes(StandardCharsets.UTF_8).length > TflMigration.MAX_BYTES) throw new IOException("Configuration exceeds 1 MiB; previous file preserved");
            Path tmp = file.resolveSibling(file.getFileName() + ".tmp");
			try (Writer writer = Files.newBufferedWriter(tmp, StandardCharsets.UTF_8)) {
				writer.write(encoded);
			}
			try {
				Files.move(tmp, file, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
			} catch (AtomicMoveNotSupportedException e) {
				Files.move(tmp, file, StandardCopyOption.REPLACE_EXISTING);
			}
			writtenSeq = seq;
		}
	}

    private boolean backupBroken() {
        try {
            Path backup = Files.exists(brokenFile()) ? file.resolveSibling(file.getFileName() + ".broken-" + java.util.UUID.randomUUID()) : brokenFile();
            Files.copy(file, backup);
            return true;
        } catch (IOException e) { return false; }
    }

    /** Preserve unknown keys and unmapped legacy data on every save/downgrade. */
    private static com.google.gson.JsonObject merge(com.google.gson.JsonObject base, com.google.gson.JsonObject current) {
        com.google.gson.JsonObject out = new com.google.gson.JsonParser().parse(base.toString()).getAsJsonObject();
        for (java.util.Map.Entry<String, com.google.gson.JsonElement> e : current.entrySet()) {
            com.google.gson.JsonElement previous = out.get(e.getKey());
            out.add(e.getKey(), previous != null && previous.isJsonObject() && e.getValue().isJsonObject()
                ? merge(previous.getAsJsonObject(), e.getValue().getAsJsonObject()) : e.getValue());
        }
        return out;
    }

    private Path transferFile(String name) throws IOException {
        if (name == null || !name.matches("[A-Za-z0-9_-]{1,64}\\.json") || name.equals(file.getFileName().toString()))
            throw new IOException("Use a separate JSON filename in the configuration directory");
        Path dir = file.toAbsolutePath().getParent();
        Path candidate = dir.resolve(name);
        if (Files.isSymbolicLink(candidate)) throw new IOException("Symbolic links are not valid imports/exports");
        return candidate;
    }

    public void exportTo(String name, ModuleRegistry registry) throws IOException {
        if (readOnly) throw new IOException("Cannot export unsupported configuration");
        Path out = transferFile(name);
        Files.createDirectories(out.getParent());
        Files.write(out, GSON.toJson(merge(preserved, GSON.toJsonTree(registry.capture()).getAsJsonObject())).getBytes(StandardCharsets.UTF_8),
            java.nio.file.StandardOpenOption.CREATE_NEW);
    }

    public void resetToDefaults(ModuleRegistry registry) throws IOException {
        if (readOnly) throw new IOException("Read-only configuration");
        flush();
        TrsConfig before = registry.capture();
        if (Files.exists(file)) Files.copy(file, file.resolveSibling(file.getFileName() + ".pre-reset-" + java.util.UUID.randomUUID() + ".bak"));
        try { registry.apply(new TrsConfig()); save(registry); }
        catch (IOException | RuntimeException e) { registry.apply(before); throw new IOException("Reset rolled back", e); }
    }

    public void importFrom(String name, ModuleRegistry registry) throws IOException {
        if (readOnly) throw new IOException("Read-only configuration");
        flush();
        com.google.gson.JsonObject raw = TflMigration.read(transferFile(name));
        if (raw.has("modules") && !raw.get("modules").isJsonObject()) throw new IOException("Import modules must be an object");
        int version;
        try { version = raw.has("configVersion") ? raw.get("configVersion").getAsInt() : 1; }
        catch (RuntimeException e) { throw new IOException("Invalid import version", e); }
        if (version < 1 || version > TrsConfig.CURRENT_VERSION) throw new IOException("Unsupported import version");
        TrsConfig incoming;
        try { incoming = GSON.fromJson(raw, TrsConfig.class); }
        catch (RuntimeException e) { throw new IOException("Invalid import", e); }
        TrsConfig before = registry.capture();
        com.google.gson.JsonObject previous = preserved;
        if (Files.exists(file)) Files.copy(file, file.resolveSibling(file.getFileName() + ".pre-import-" + java.util.UUID.randomUUID() + ".bak"));
        try { registry.apply(incoming); preserved = raw; save(registry); }
        catch (IOException | RuntimeException e) { registry.apply(before); preserved = previous; throw new IOException("Import rolled back", e); }
    }
}
