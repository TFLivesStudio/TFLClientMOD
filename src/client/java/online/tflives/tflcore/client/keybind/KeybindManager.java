package online.tflives.tflcore.client.keybind;

import com.mojang.blaze3d.platform.InputConstants;
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import net.minecraft.ChatFormatting;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import online.tflives.tflcore.client.TflCoreClient;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * Single place that owns TFL Core keybinds.
 *
 * <p>Right Shift remains the default for opening the settings screen. Bindings are registered in
 * vanilla Controls so they can be rebound there, and the in-game editor reports conflicts with
 * vanilla or other mods so the user can fix them.</p>
 */
public final class KeybindManager {
    public enum Action {
        SETTINGS("key.tflcore.settings", InputConstants.KEY_RSHIFT, false),
        HUD_EDITOR("key.tflcore.hud_editor", InputConstants.KEY_H, false),
        ZOOM("key.tflcore.zoom", InputConstants.KEY_C, true),
        WAYPOINT("key.tflcore.waypoint", InputConstants.KEY_B, false),
        DIAGNOSTICS("key.tflcore.diagnostics", InputConstants.KEY_F9, false);

        private final String translationKey;
        private final int defaultKey;
        private final boolean hold;

        Action(String translationKey, int defaultKey, boolean hold) {
            this.translationKey = translationKey;
            this.defaultKey = defaultKey;
            this.hold = hold;
        }

        public String translationKey() {
            return translationKey;
        }

        public boolean hold() {
            return hold;
        }
    }

    public static final class Conflict {
        public final Action action;
        public final String otherBinding;

        Conflict(Action action, String otherBinding) {
            this.action = action;
            this.otherBinding = otherBinding;
        }
    }

    private final Map<Action, KeyMapping> mappings = new EnumMap<>(Action.class);
    private Action rebinding;
    private KeyMapping.Category category;

    public KeyMapping.Category category() {
        return category;
    }

    public void register() {
        if (category == null) {
            category = KeyMapping.Category.register(
                    Identifier.fromNamespaceAndPath(TflCoreClient.MOD_ID, "controls"));
        }
        for (Action action : Action.values()) {
            KeyMapping mapping = KeyMappingHelper.registerKeyMapping(new KeyMapping(
                    action.translationKey,
                    InputConstants.Type.KEYBOARD,
                    action.defaultKey,
                    category));
            mappings.put(action, mapping);
        }
    }

    public KeyMapping mapping(Action action) {
        return mappings.get(action);
    }

    public boolean isDown(Action action) {
        KeyMapping mapping = mappings.get(action);
        return mapping != null && mapping.isDown();
    }

    public boolean consume(Action action) {
        KeyMapping mapping = mappings.get(action);
        return mapping != null && mapping.consumeClick();
    }

    public void resetDefaults() {
        for (Map.Entry<Action, KeyMapping> entry : mappings.entrySet()) {
            entry.getValue().setKey(InputConstants.Type.KEYBOARD.getOrCreate(entry.getKey().defaultKey));
        }
        Minecraft client = Minecraft.getInstance();
        if (client != null && client.options != null) {
            client.options.save();
        }
    }

    public boolean isRebinding() {
        return rebinding != null;
    }

    public Action rebindingAction() {
        return rebinding;
    }

    public void beginRebind(Action action) {
        rebinding = action;
    }

    public void cancelRebind() {
        rebinding = null;
    }

    /** Handles a key while the settings screen waits for a rebind. Returns true when consumed. */
    public boolean handleRebindKey(Minecraft client, KeyEvent event) {
        if (rebinding == null) return false;
        if (event.key() == InputConstants.KEY_ESCAPE) {
            cancelRebind();
            return true;
        }
        InputConstants.Key key = InputConstants.getKey(event);
        KeyMapping mapping = mappings.get(rebinding);
        if (mapping != null && key != InputConstants.UNKNOWN) {
            mapping.setKey(key);
            if (client != null && client.options != null) {
                client.options.save();
            }
        }
        rebinding = null;
        return true;
    }

    /**
     * Returns conflicts for TFL bindings against every registered vanilla/mod keybinding.
     * A conflict means the same physical key is bound to two actions.
     */
    public List<Conflict> conflicts(Minecraft client) {
        List<Conflict> result = new ArrayList<>();
        if (client == null || client.options == null) return result;
        for (Map.Entry<Action, KeyMapping> entry : mappings.entrySet()) {
            KeyMapping mapping = entry.getValue();
            if (mapping.isUnbound()) continue;
            String own = mapping.saveString();
            for (KeyMapping other : client.options.keyMappings) {
                if (other == mapping || other.isUnbound()) continue;
                if (own.equals(other.saveString())) {
                    result.add(new Conflict(entry.getKey(), other.getName()));
                    break;
                }
            }
        }
        return result;
    }

    public static Component describe(KeyMapping mapping) {
        if (mapping == null || mapping.isUnbound()) {
            return Component.translatable("tflcore.key.unbound").withStyle(ChatFormatting.GRAY);
        }
        return mapping.getTranslatedKeyMessage().copy().withStyle(ChatFormatting.WHITE);
    }
}
