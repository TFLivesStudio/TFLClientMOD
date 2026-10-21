package online.tflives.tflcore.client.survival;

import net.minecraft.client.Minecraft;
import online.tflives.tflcore.client.config.SettingsStore;
import online.tflives.tflcore.client.keybind.KeybindManager;
import online.tflives.tflcore.client.module.ModuleCategory;
import online.tflives.tflcore.client.module.TflModule;

/**
 * Hold-to-zoom implemented through the vanilla FOV option, with an exact restore of the user's
 * original value. No mixin and no renderer patch are required.
 */
public final class ZoomModule implements TflModule {
    private final KeybindManager keybinds;
    private final SettingsStore store;
    private boolean zooming;
    private int originalFov = -1;

    public ZoomModule(KeybindManager keybinds, SettingsStore store) {
        this.keybinds = keybinds;
        this.store = store;
    }

    @Override
    public String id() {
        return "zoom";
    }

    @Override
    public ModuleCategory category() {
        return ModuleCategory.SURVIVAL;
    }

    @Override
    public void onTick(Minecraft client) {
        boolean canZoom = client.player != null
                && client.gui.screen() == null
                && !client.player.isSpectator();
        boolean down = canZoom && keybinds.isDown(KeybindManager.Action.ZOOM);

        if (!down) {
            restore(client);
            return;
        }
        if (!zooming) {
            originalFov = client.options.fov().get();
            zooming = true;
        }
        int target = store.get().survival.zoomFov;
        int current = client.options.fov().get();
        int next;
        if (store.get().survival.smoothZoom) {
            next = Math.round(current + (target - current) * 0.45F);
            if (Math.abs(next - target) <= 1) {
                next = target;
            }
        } else {
            next = target;
        }
        if (next != current) {
            client.options.fov().set(next);
        }
    }

    @Override
    public void onEnable(Minecraft client) {
        store.get().survival.zoom = true;
        store.requestSave();
    }

    @Override
    public void onDisable(Minecraft client) {
        restore(client);
        store.get().survival.zoom = false;
        store.requestSave();
    }

    @Override
    public void onWorldLeave(Minecraft client) {
        restore(client);
    }

    private void restore(Minecraft client) {
        if (!zooming || client == null) return;
        if (originalFov > 0) {
            client.options.fov().set(originalFov);
        }
        originalFov = -1;
        zooming = false;
    }
}
