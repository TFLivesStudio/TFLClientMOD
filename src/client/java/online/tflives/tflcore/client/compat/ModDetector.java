package online.tflives.tflcore.client.compat;

import net.fabricmc.loader.api.FabricLoader;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Optional detection of well-known client mods. Detection is informational only: TFL Core never
 * requires, edits or reconfigures another mod.
 */
public final class ModDetector {
    private static final Map<String, String> KNOWN = new LinkedHashMap<>();

    static {
        KNOWN.put("sodium", "Sodium");
        KNOWN.put("iris", "Iris");
        KNOWN.put("lithium", "Lithium");
        KNOWN.put("ferritecore", "FerriteCore");
        KNOWN.put("fabric-api", "Fabric API");
        KNOWN.put("modmenu", "Mod Menu");
    }

    private ModDetector() {
    }

    public static boolean isLoaded(String id) {
        try {
            return FabricLoader.getInstance().isModLoaded(id);
        } catch (Throwable ignored) {
            return false;
        }
    }

    public static Map<String, Boolean> snapshot() {
        Map<String, Boolean> result = new LinkedHashMap<>();
        for (Map.Entry<String, String> entry : KNOWN.entrySet()) {
            result.put(entry.getValue(), isLoaded(entry.getKey()));
        }
        return result;
    }

    public static String fabricLoaderVersion() {
        return FabricLoader.getInstance().getModContainer("fabricloader")
                .map(container -> container.getMetadata().getVersion().getFriendlyString())
                .orElse("?");
    }

    public static String minecraftVersion() {
        return FabricLoader.getInstance().getModContainer("minecraft")
                .map(container -> container.getMetadata().getVersion().getFriendlyString())
                .orElse("?");
    }
}
