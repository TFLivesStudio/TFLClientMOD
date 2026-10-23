package dev.theredstonee.trsclient.core.hud;

import dev.theredstonee.trsclient.core.module.HudModule;
import dev.theredstonee.trsclient.core.module.Module;
import dev.theredstonee.trsclient.core.module.ModuleRegistry;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

/** Explicit starting points. Applying one always creates a copy, preserving existing profiles. */
public final class HudPresets {
    public enum Preset {
        MINIMAL("Minimal", "fps"),
        PVP("PvP", "fps", "cps", "ping", "keystrokes", "armor", "effects", "combo", "reach"),
        SURVIVAL("Survival", "fps", "coords", "armor", "effects", "clock"),
        BUILDER("Builder", "fps", "coords", "redstoneSignal", "redstoneClock"),
        EXPLORATION("Exploration", "coords", "minimap", "speed", "clock"),
        CUSTOM("Custom");

        public final String label;
        private final Set<String> widgets;
        Preset(String label, String... widgets) {
            this.label = label;
            this.widgets = new HashSet<String>(Arrays.asList(widgets));
        }
    }

    private HudPresets() { }

    /** Returns a localized error without touching the current layout when capacity is exhausted. */
    public static String create(HudProfiles profiles, ModuleRegistry registry, Preset preset) {
        String name = preset.label;
        int suffix = 2;
        while (contains(profiles, name)) name = preset.label + " " + suffix++;
        String error = profiles.create(name);
        if (error != null || preset == Preset.CUSTOM) return error;
        for (Module module : registry.all()) {
            if (!(module instanceof HudModule)) continue;
            HudModule hud = (HudModule) module;
            // Toggle-sprint/sneak are gameplay controls as well as widgets; never alter them here.
            if ("toggleSprint".equals(hud.id()) || "toggleSneak".equals(hud.id())) continue;
            hud.setEnabled(hud.available() && preset.widgets.contains(hud.id()));
            if (hud.isEnabled()) hud.resetLayout();
        }
        return null;
    }

    private static boolean contains(HudProfiles profiles, String name) {
        for (String existing : profiles.names()) if (existing.equalsIgnoreCase(name)) return true;
        return false;
    }
}
