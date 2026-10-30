package dev.theredstonee.trsclient.core.map.session;

import com.google.gson.Gson;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;

/** Persistent subworlds. A dimension or server address alone never identifies a remote world. */
public final class WorldCatalog {
  public static final class Entry {
    public String id, name;
    public String backendKey;
    public int turns;

    public Entry() {}

    Entry(String id, String name) {
      this.id = id;
      this.name = name;
    }
  }

  private static final class Data {
    Map<String, List<Entry>> servers = new LinkedHashMap<String, List<Entry>>();
  }

  private final Path file;
  private Data data = new Data();
  private Object context;
  private String server = "", active = "";
  private boolean manual;

  public WorldCatalog(Path file) throws IOException {
    this.file = file;
    if (Files.isRegularFile(file))
      try (Reader reader = Files.newBufferedReader(file, StandardCharsets.UTF_8)) {
        Data loaded = new Gson().fromJson(reader, Data.class);
        if (loaded != null && loaded.servers != null) data = loaded;
      } catch (RuntimeException invalid) {
        throw new IOException("Invalid map world catalog; original file preserved", invalid);
      }
  }

  public String begin(Object level, String origin, String providedId) throws IOException {
    if (level == null) {
      context = null;
      active = "";
      server = "";
      return "";
    }
    if (level == context
        && origin.equals(server)
        && !active.isEmpty()
        && (manual || providedId == null || active.equals("server-" + providedId)))
      return namespace();
    context = level;
    server = origin;
    manual = false;
    if (providedId != null && !providedId.isEmpty()) {
      active = "server-" + providedId;
      ensure(
          active,
          dev.theredstonee.trsclient.core.i18n.I18n.tr("serverMaps.serverWorld", providedId));
    } else {
      create();
      manual = false;
    }
    return namespace();
  }

  public void addExisting(String id, String name, String backend) throws IOException {
    for (Entry e : list()) if (e.id.equals(id)) return;
    Entry e = new Entry(id, name);
    e.backendKey = backend;
    list().add(e);
    save();
  }

  public String namespace() {
    return active.isEmpty() ? "" : server + "/world/" + active;
  }

  public String activeId() {
    return active;
  }

  public List<Entry> entries() {
    return Collections.unmodifiableList(new ArrayList<Entry>(list()));
  }

  public void select(String id) {
    for (Entry e : list())
      if (e.id.equals(id)) {
        active = id;
        manual = true;
        return;
      }
    throw new IllegalArgumentException("Unknown subworld");
  }

  public void create() throws IOException {
    manual = true;
    active = UUID.randomUUID().toString();
    ensure(
        active,
        dev.theredstonee.trsclient.core.i18n.I18n.tr("serverMaps.world", list().size() + 1));
  }

  public void rename(String name) throws IOException {
    String n = name == null ? "" : name.trim();
    if (n.isEmpty() || n.length() > 64)
      throw new IllegalArgumentException("World name must contain 1–64 characters");
    for (Entry e : list()) if (e.id.equals(active)) e.name = n;
    save();
  }

  public int turns() {
    for (Entry e : list()) if (e.id.equals(active)) return e.turns & 3;
    return 0;
  }

  public void rotate(int delta) throws IOException {
    for (Entry e : list()) if (e.id.equals(active)) e.turns = (e.turns + delta) & 3;
    save();
  }

  public String backendKey(String fallback) throws IOException {
    for (Entry e : list())
      if (e.id.equals(active)) {
        if (e.backendKey == null || e.backendKey.isEmpty()) {
          e.backendKey = fallback;
          save();
        }
        return e.backendKey;
      }
    return fallback;
  }

  public String name() {
    for (Entry e : list()) if (e.id.equals(active)) return e.name;
    return "";
  }

  private List<Entry> list() {
    List<Entry> list = data.servers.get(server);
    if (list == null) {
      list = new ArrayList<Entry>();
      data.servers.put(server, list);
    }
    return list;
  }

  private void ensure(String id, String name) throws IOException {
    for (Entry e : list()) if (e.id.equals(id)) return;
    list().add(new Entry(id, name));
    save();
  }

  private void save() throws IOException {
    Files.createDirectories(file.toAbsolutePath().getParent());
    Path temp = file.resolveSibling(file.getFileName() + ".tmp");
    try (Writer writer = Files.newBufferedWriter(temp, StandardCharsets.UTF_8)) {
      new Gson().toJson(data, writer);
    }
    try {
      Files.move(temp, file, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
    } catch (AtomicMoveNotSupportedException e) {
      Files.move(temp, file, StandardCopyOption.REPLACE_EXISTING);
    }
  }
}
