package online.tflives.tflcore.client.module;

import net.minecraft.network.chat.Component;

/** Stable module categories used by the settings UI and diagnostics screen. */
public enum ModuleCategory {
    CORE("tflcore.category.core"),
    VISUAL("tflcore.category.visual"),
    HUD("tflcore.category.hud"),
    PVP("tflcore.category.pvp"),
    SURVIVAL("tflcore.category.survival"),
    TECHNICAL("tflcore.category.technical"),
    PERFORMANCE("tflcore.category.performance");

    private final String translationKey;

    ModuleCategory(String translationKey) {
        this.translationKey = translationKey;
    }

    public String translationKey() {
        return translationKey;
    }

    public Component label() {
        return Component.translatable(translationKey);
    }
}
