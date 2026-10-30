package dev.theredstonee.trsclient.core.waypoint;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonParseException;
import com.google.gson.reflect.TypeToken;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Wegpunkte je Welt/Server in {@code config/trsclient-waypoints.json}.
 * Schlüssel ist die Welt ({@code sp:<Weltordner>} bzw. {@code mp:<adresse>}),
 * die Dimension steht am Wegpunkt – so bleiben Nether-Punkte in der Oberwelt unsichtbar.
 */
public final class WaypointStore {
	private static final Gson GSON = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();
	private static final Type TYPE = new TypeToken<Map<String, List<Waypoint>>>() {
	}.getType();
	/** Sicherheitsgrenze pro Welt. */
	public static final int MAX_PER_WORLD = 200;

	private final Path file;
	private final Map<String, List<Waypoint>> worlds = new LinkedHashMap<>();
	private boolean dirty;

	public WaypointStore(Path file) {
		this.file = file;
	}

    /** Diorama's per-world array format, with stable IDs, icons, kinds and folders. */
    public void importDiorama(Path file,String worldKey) throws IOException {
      if(!Files.isRegularFile(file))return;if(Files.size(file)>4*1024*1024)throw new IOException("Waypoint file too large");
      List<Waypoint> imported;
      try(Reader reader=Files.newBufferedReader(file,StandardCharsets.UTF_8)){imported=GSON.fromJson(reader,new TypeToken<List<Waypoint>>(){}.getType());}
      catch(RuntimeException e){throw new IOException("Invalid Diorama waypoints; original preserved",e);}
      if(imported==null)return;List<Waypoint> safe=new ArrayList<>();for(Waypoint point:imported)if(point!=null){point.normalized();if("minecraft:overworld".equals(point.dimension))point.dimension="dim0";else if("minecraft:the_nether".equals(point.dimension))point.dimension="dim-1";else if("minecraft:the_end".equals(point.dimension))point.dimension="dim1";point.death="DEATH".equals(point.kind);if(!"TEMPORARY".equals(point.kind))safe.add(point);}
      worlds.put(worldKey,safe);dirty=true;
    }
    public void exportDiorama(Path file,String worldKey) throws IOException {
      Files.createDirectories(file.toAbsolutePath().getParent());Path temporary=file.resolveSibling(file.getFileName()+".tmp");
      com.google.gson.JsonArray list=new com.google.gson.JsonArray();
      for(Waypoint point:all(worldKey))if(!point.temporary){com.google.gson.JsonObject json=GSON.toJsonTree(point).getAsJsonObject();String dimension=point.dimension;if("dim0".equals(dimension))dimension="minecraft:overworld";else if("dim-1".equals(dimension))dimension="minecraft:the_nether";else if("dim1".equals(dimension))dimension="minecraft:the_end";json.addProperty("dimension",dimension);json.addProperty("color",0xff000000|point.color);json.addProperty("kind",point.death?"DEATH":point.kind);list.add(json);}
      try(Writer writer=Files.newBufferedWriter(temporary,StandardCharsets.UTF_8)){GSON.toJson(list,writer);}
      try{Files.move(temporary,file,StandardCopyOption.REPLACE_EXISTING,StandardCopyOption.ATOMIC_MOVE);}catch(AtomicMoveNotSupportedException e){Files.move(temporary,file,StandardCopyOption.REPLACE_EXISTING);}
    }

	public Path file() {
		return file;
	}

	/** Weltschlüssel für einen Server. */
	public static String serverKey(String address) {
		return "mp:" + (address == null ? "?" : address.toLowerCase(java.util.Locale.ROOT));
	}

	/** Weltschlüssel für eine Einzelspielerwelt. */
	public static String singleplayerKey(String levelId) {
		return "sp:" + (levelId == null ? "?" : levelId);
	}

	/** Lädt die Datei; fehlt oder ist sie kaputt, wird mit einer leeren Liste weitergemacht. */
	public void load() {
		worlds.clear();
		if (!Files.exists(file)) return;
		try (Reader reader = Files.newBufferedReader(file, StandardCharsets.UTF_8)) {
			Map<String, List<Waypoint>> data = GSON.fromJson(reader, TYPE);
			if (data != null) {
				for (Map.Entry<String, List<Waypoint>> e : data.entrySet()) {
					if (e.getKey() == null || e.getValue() == null) continue;
					List<Waypoint> list = new ArrayList<>();
					for (Waypoint w : e.getValue()) {
						if (w != null) list.add(w.normalized());
					}
					worlds.put(e.getKey(), list);
				}
			}
		} catch (JsonParseException | IllegalStateException | ClassCastException | IOException e) {
			worlds.clear();
		}
	}

	/** Schreibt die Datei atomar (nur wenn etwas geändert wurde). */
	public void saveIfDirty() throws IOException {
		if (!dirty) return;
		save();
	}

	public void save() throws IOException {
		Path dir = file.toAbsolutePath().getParent();
		if (dir != null) Files.createDirectories(dir);
		Path tmp = file.resolveSibling(file.getFileName() + ".tmp");
		// Vorübergehende Wegpunkte (Notiz-Koordinaten) bleiben nur im Speicher.
		Map<String, List<Waypoint>> kept = new LinkedHashMap<>();
		for (Map.Entry<String, List<Waypoint>> e : worlds.entrySet()) {
			List<Waypoint> list = new ArrayList<>();
			for (Waypoint w : e.getValue()) {
				if (!w.temporary) list.add(w);
			}
			if (!list.isEmpty() || e.getValue().isEmpty()) kept.put(e.getKey(), list);
		}
		try (Writer writer = Files.newBufferedWriter(tmp, StandardCharsets.UTF_8)) {
			GSON.toJson(kept, TYPE, writer);
		}
		try {
			Files.move(tmp, file, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
		} catch (AtomicMoveNotSupportedException e) {
			Files.move(tmp, file, StandardCopyOption.REPLACE_EXISTING);
		}
		dirty = false;
	}

	/** Alle Wegpunkte einer Welt (unveränderliche Sicht). */
	public List<Waypoint> all(String worldKey) {
		List<Waypoint> list = worlds.get(worldKey);
		return list == null ? Collections.<Waypoint>emptyList() : Collections.unmodifiableList(list);
	}

	/** Sichtbare Wegpunkte einer Welt in dieser Dimension. */
	public List<Waypoint> visible(String worldKey, String dimension) {
		List<Waypoint> out = new ArrayList<>();
		for (Waypoint w : all(worldKey)) {
			if (w.visible && w.inDimension(dimension)) out.add(w);
		}
		return out;
	}

	/** Fügt einen Wegpunkt hinzu (ältester fällt raus, wenn die Grenze erreicht ist). */
	public Waypoint add(String worldKey, Waypoint waypoint) {
		List<Waypoint> list = worlds.get(worldKey);
		if (list == null) {
			list = new ArrayList<>();
			worlds.put(worldKey, list);
		}
		if (waypoint.created == 0) waypoint.created = System.currentTimeMillis();
		list.add(waypoint.normalized());
		while (list.size() > MAX_PER_WORLD) list.remove(0);
		dirty = true;
		return waypoint;
	}

	/** Legt einen Todespunkt an und entfernt den vorherigen (nur der letzte Tod zählt). */
	public Waypoint setDeath(String worldKey, int x, int y, int z, String dimension, int color) {
		List<Waypoint> list = worlds.get(worldKey);
		if (list != null) {
			for (int i = list.size() - 1; i >= 0; i--) {
				if (list.get(i).death) list.remove(i);
			}
		}
		Waypoint death = new Waypoint(dev.theredstonee.trsclient.core.i18n.I18n.tr("waypoint.deathName"), x, y, z, dimension, color);
		death.death = true;
		return add(worldKey, death);
	}

	public boolean remove(String worldKey, Waypoint waypoint) {
		List<Waypoint> list = worlds.get(worldKey);
		if (list == null || !list.remove(waypoint)) return false;
		dirty = true;
		return true;
	}

	/** Meldet eine Änderung an einem Wegpunkt (Name/Farbe/sichtbar). */
	public void touch() {
		dirty = true;
	}

	public boolean dirty() {
		return dirty;
	}

	public int worldCount() {
		return worlds.size();
	}
}
