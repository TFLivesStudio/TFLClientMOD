package dev.theredstonee.trsclient.core.config;

import com.google.gson.*;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;

/** TFL 2026: non-destructive, one-time import into a separate adopted-client configuration. */
public final class TflMigration {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    public static final long MAX_BYTES = 1024 * 1024;
    private TflMigration() {}

    public static JsonObject read(Path source) throws IOException {
        if (Files.size(source) > MAX_BYTES) throw new IOException("Configuration exceeds 1 MiB: " + source.getFileName());
        try {
            JsonElement tree = new JsonParser().parse(new String(Files.readAllBytes(source), StandardCharsets.UTF_8));
            if (!tree.isJsonObject()) throw new IOException("Configuration root must be an object");
            return tree.getAsJsonObject();
        } catch (RuntimeException e) { throw new IOException("Invalid configuration: " + source.getFileName(), e); }
    }

    public static void prepare(Path target) throws IOException {
        if (Files.exists(target) || !target.getFileName().toString().equals("client.json") ||
                target.getParent() == null || !target.getParent().getFileName().toString().equals("tflcore")) return;
        Path dir = target.toAbsolutePath().getParent().getParent();
        Path source = dir.resolve("tflcore.json");
        boolean oldTfl = Files.exists(source);
        if (!oldTfl) source = dir.resolve("trsclient.json");
        if (!Files.exists(source)) return;
        JsonObject raw = read(source);
        JsonObject converted;
        JsonObject waypoints = new JsonObject();
        if (oldTfl) {
            if (!raw.has("schemaVersion") || raw.get("schemaVersion").getAsDouble() != 2)
                throw new IOException("Only TFL schema v2 is supported; original kept untouched");
            converted = convert(raw);
            JsonObject survival = object(raw, "survival");
            if (survival.has("waypoints")) {
                for (JsonElement e : survival.getAsJsonArray("waypoints")) {
                    JsonObject w = e.getAsJsonObject();
                    String world = string(w, "world", "");
                    if (world.startsWith("server:")) world = "mp:" + world.substring(7).toLowerCase(java.util.Locale.ROOT);
                    // World identity must be known; never attach an ambiguous marker to another world.
                    if (!world.startsWith("sp:") && !world.startsWith("mp:")) continue;
                    JsonObject copy = new com.google.gson.JsonParser().parse(w.toString()).getAsJsonObject();
                    copy.addProperty("visible", !w.has("enabled") || w.get("enabled").getAsBoolean());
                    for (String axis : new String[]{"x", "y", "z"}) {
                        double coordinate = w.get(axis).getAsDouble();
                        if (!Double.isFinite(coordinate) || coordinate < -30000000 || coordinate > 30000000)
                            throw new IOException("Invalid waypoint coordinate");
                        copy.addProperty(axis, (int)Math.floor(coordinate));
                    }
                    if (!waypoints.has(world)) waypoints.add(world, new JsonArray());
                    waypoints.getAsJsonArray(world).add(copy);
                }
            }
        } else {
            int version = raw.has("configVersion") ? raw.get("configVersion").getAsInt() : 1;
            if (version < 1 || version > TrsConfig.CURRENT_VERSION) throw new IOException("Unsupported TRS config version");
            converted = new com.google.gson.JsonParser().parse(raw.toString()).getAsJsonObject();
            Path oldWaypoints = dir.resolve("trsclient-waypoints.json");
            if (Files.exists(oldWaypoints)) waypoints = read(oldWaypoints);
        }
        // Validate before touching any destination. Keep all unconvertible/unknown fields in the original backup.
        GSON.fromJson(converted, TrsConfig.class);
        if (GSON.toJson(converted).getBytes(StandardCharsets.UTF_8).length > MAX_BYTES) throw new IOException("Converted configuration exceeds 1 MiB");
        Path markerTarget = target.resolveSibling("waypoints.json");
        if (waypoints.entrySet().size() > 0 && Files.exists(markerTarget)) throw new IOException("Waypoint destination already exists; manual merge required");
        Files.copy(source, source.resolveSibling(source.getFileName() + ".pre-adoption-" + UUID.randomUUID() + ".bak"));
        Files.createDirectories(target.getParent());
        boolean wroteMarkers = false;
        try {
            if (waypoints.entrySet().size() > 0) { writeNew(markerTarget, waypoints); wroteMarkers = true; }
            writeNew(target, converted); // Commit last. A failed import never installs a partial client config.
        } catch (IOException | RuntimeException e) {
            if (wroteMarkers) Files.deleteIfExists(markerTarget);
            throw e;
        }
    }

    private static void writeNew(Path path, JsonObject value) throws IOException {
        Path tmp = Files.createTempFile(path.getParent(), ".tfl-import-", ".tmp");
        try {
            Files.write(tmp, GSON.toJson(value).getBytes(StandardCharsets.UTF_8));
            Files.move(tmp, path); // No REPLACE_EXISTING: never overwrite another writer/user file.
        } finally { Files.deleteIfExists(tmp); }
    }

    public static JsonObject convert(JsonObject old) {
        JsonObject result = new JsonObject();
        result.addProperty("configVersion", TrsConfig.CURRENT_VERSION);
        result.add("tflLegacyV2", new com.google.gson.JsonParser().parse(old.toString()).getAsJsonObject());
        JsonObject modules = new JsonObject();
        result.add("modules", modules);
        Map<String,String> ids = new LinkedHashMap<>();
        for (String id : new String[]{"fps","cps","keystrokes","ping","armor","effects","clock","speed","zoom","waypoints","crosshair"}) ids.put(id,id);
        ids.put("coordinates","coords"); ids.put("coords","coords"); ids.put("toggle_sprint","toggleSprint"); ids.put("toggleSprint","toggleSprint");
        JsonObject states = object(old,"modules");
        for (Map.Entry<String,JsonElement> e : states.entrySet()) {
            String id = ids.get(e.getKey());
            if (id != null && e.getValue().isJsonPrimitive() && e.getValue().getAsJsonPrimitive().isBoolean()) {
                JsonObject m = new JsonObject(); m.add("enabled",e.getValue()); modules.add(id,m);
            }
        }
        JsonObject hud = object(old,"hud");
        for (Map.Entry<String,JsonElement> e : object(hud,"widgets").entrySet()) {
            String id = ids.get(e.getKey()); if (id == null) continue;
            JsonObject w = e.getValue().getAsJsonObject();
            JsonObject m = modules.has(id) ? modules.getAsJsonObject(id) : new JsonObject(); modules.add(id,m);
            boolean enabled = !w.has("enabled") || w.get("enabled").getAsBoolean();
            if (hud.has("enabled") && !hud.get("enabled").getAsBoolean()) enabled = false;
            m.addProperty("enabled", enabled);
            // Pixel offsets cannot be converted to relative offsets without the original resolution.
            // Preserve them in tflLegacyV2 and keep safe upstream anchors/positions until HUD editor confirmation.
            if (w.has("anchor")) m.add("anchor",w.get("anchor"));
            JsonObject numbers = new JsonObject(); m.add("numbers",numbers);
            if(w.has("scale")) numbers.add("scale",w.get("scale"));
            if(w.has("opacity")) numbers.addProperty("backgroundOpacity",w.get("opacity").getAsDouble()*100);
            JsonObject flags = new JsonObject(); m.add("flags",flags);
            if(w.has("background")) flags.add("background",w.get("background"));
        }
        JsonObject offline = new JsonObject(); offline.addProperty("enabled",false); modules.add("trsOnline",offline);
        result.addProperty("keyDefaults",KeyDefaults.CURRENT);
        return result;
    }
    private static JsonObject object(JsonObject root,String key) {
        return root.has(key) && root.get(key).isJsonObject() ? root.getAsJsonObject(key) : new JsonObject();
    }
    private static String string(JsonObject root,String key,String fallback) {
        return root.has(key) && root.get(key).isJsonPrimitive() ? root.get(key).getAsString() : fallback;
    }
}
