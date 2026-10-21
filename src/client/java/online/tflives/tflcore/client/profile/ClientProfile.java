package online.tflives.tflcore.client.profile;

import java.util.LinkedHashSet;
import java.util.Set;

/**
 * Gameplay-oriented module sets. Applying a profile only touches TFL Core modules and widgets; it
 * never changes Sodium, vanilla graphics or another mod's configuration.
 */
public enum ClientProfile {
    PVP("tflcore.profile.pvp", Set.of("crosshair", "toggle-sprint", "zoom", "waypoints"),
            Set.of("fps", "cps", "keystrokes", "ping", "armor", "sprint", "helditem",
                    "potions", "target", "frametime")),
    SURVIVAL("tflcore.profile.survival", Set.of("zoom", "waypoints"),
            Set.of("fps", "coordinates", "direction", "biome", "potions", "armor",
                    "worldtime", "clock", "waypoint")),
    BUILDER("tflcore.profile.builder", Set.of("waypoints"),
            Set.of("coordinates", "direction", "chunk", "target", "helditem", "worldtime")),
    EXPLORATION("tflcore.profile.exploration", Set.of("zoom", "waypoints"),
            Set.of("coordinates", "direction", "biome", "waypoint", "worldtime", "armor", "fps")),
    MINIMAL("tflcore.profile.minimal", Set.of(),
            Set.of("fps", "coordinates")),
    CUSTOM("tflcore.profile.custom", Set.of(), Set.of());

    private final String translationKey;
    private final Set<String> modules;
    private final Set<String> widgets;

    ClientProfile(String translationKey, Set<String> modules, Set<String> widgets) {
        this.translationKey = translationKey;
        this.modules = new LinkedHashSet<>(modules);
        this.widgets = new LinkedHashSet<>(widgets);
    }

    public String translationKey() {
        return translationKey;
    }

    public Set<String> modules() {
        return Set.copyOf(modules);
    }

    public Set<String> widgets() {
        return Set.copyOf(widgets);
    }

    public boolean isAutomatic() {
        return this != CUSTOM;
    }

    public ClientProfile next() {
        ClientProfile[] values = values();
        ClientProfile next = values[(ordinal() + 1) % values.length];
        // CUSTOM is an automatic fallback, never an explicit selection.
        return next == CUSTOM ? PVP : next;
    }

    public static ClientProfile fromName(String name) {
        if (name != null) {
            for (ClientProfile profile : values()) {
                if (profile.name().equalsIgnoreCase(name)) return profile;
            }
        }
        return CUSTOM;
    }
}
