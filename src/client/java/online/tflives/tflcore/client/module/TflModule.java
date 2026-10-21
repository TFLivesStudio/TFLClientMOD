package online.tflives.tflcore.client.module;

import net.minecraft.client.Minecraft;

import java.util.List;

/**
 * A self-contained, optional TFL Core feature.
 *
 * <p>Modules are registered once during client initialization. Everything else (ordering,
 * dependency validation, enable/disable, failure isolation) is handled by {@link ModuleManager}.
 * Implementations must be cheap when disabled: {@code onTick} is only called while enabled.</p>
 */
public interface TflModule {
    /** Stable, lower-case id persisted in settings. Never change it once released. */
    String id();

    ModuleCategory category();

    default String nameKey() {
        return "tflcore.module." + id() + ".name";
    }

    default String descriptionKey() {
        return "tflcore.module." + id() + ".desc";
    }

    /** Module ids that must be enabled for this module to work. */
    default List<String> requires() {
        return List.of();
    }

    default boolean enabledByDefault() {
        return true;
    }

    /** Whether the user is allowed to toggle this module. Core modules can still be disabled. */
    default boolean configurable() {
        return true;
    }

    default void onEnable(Minecraft client) {
    }

    default void onDisable(Minecraft client) {
    }

    default void onTick(Minecraft client) {
    }

    default void onWorldJoin(Minecraft client) {
    }

    default void onWorldLeave(Minecraft client) {
    }
}
