package online.tflives.tflcore.client.pvp;

import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import online.tflives.tflcore.client.config.SettingsStore;
import online.tflives.tflcore.client.module.ModuleCategory;
import online.tflives.tflcore.client.module.TflModule;

/**
 * Keeps sprint held while the player moves forward.
 *
 * <p>This only mirrors a normal sprint key press through the vanilla input state; it does not send
 * packets, change reach, or automate combat. The module releases the key the moment forward is
 * released, the player sneaks, a screen opens, the module is disabled or the world unloads.</p>
 */
public final class ToggleSprintModule implements TflModule {
    private final SettingsStore store;
    private boolean held;

    public ToggleSprintModule(SettingsStore store) {
        this.store = store;
    }

    @Override
    public String id() {
        return "toggle-sprint";
    }

    @Override
    public ModuleCategory category() {
        return ModuleCategory.PVP;
    }

    @Override
    public boolean enabledByDefault() {
        return false;
    }

    @Override
    public void onTick(Minecraft client) {
        if (client.player == null) {
            release(client);
            return;
        }
        LocalPlayer player = client.player;
        boolean shouldHold = client.gui.screen() == null
                && client.options.keyUp.isDown()
                && !player.isCrouching()
                && !client.options.keyShift.isDown()
                && player.getVehicle() == null;
        if (shouldHold == held) return;
        held = shouldHold;
        client.options.keySprint.setDown(shouldHold);
    }

    @Override
    public void onEnable(Minecraft client) {
        store.get().pvp.toggleSprint = true;
        store.requestSave();
    }

    @Override
    public void onDisable(Minecraft client) {
        release(client);
        store.get().pvp.toggleSprint = false;
        store.requestSave();
    }

    @Override
    public void onWorldLeave(Minecraft client) {
        release(client);
    }

    private void release(Minecraft client) {
        if (held && client != null) {
            client.options.keySprint.setDown(false);
        }
        held = false;
    }
}
