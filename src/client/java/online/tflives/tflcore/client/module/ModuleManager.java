package online.tflives.tflcore.client.module;

import net.minecraft.client.Minecraft;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;

/**
 * Central module registry with dependency validation and failure isolation.
 *
 * <p>Rules:</p>
 * <ul>
 *   <li>Duplicate ids are rejected at registration time.</li>
 *   <li>Missing dependencies disable the dependent module instead of crashing.</li>
 *   <li>Cycles are detected and reported; affected modules stay disabled.</li>
 *   <li>Disabling a module also disables the modules that depend on it.</li>
 *   <li>A module that throws is disabled and recorded; one broken feature never takes down the
 *       client, but the failure stays visible in diagnostics.</li>
 * </ul>
 */
public final class ModuleManager {
    private static final Logger LOGGER = LoggerFactory.getLogger("tflcore");

    public record ModuleError(String module, String message, long timestamp) {
        public String shortMessage() {
            if (message == null) return "unknown error";
            return message.length() > 160 ? message.substring(0, 157) + "..." : message;
        }
    }

    public record ModuleInfo(String id, ModuleCategory category, ModuleState state,
                             List<String> requires, boolean enabledByDefault, boolean configurable) {
    }

    private final Map<String, TflModule> modules = new LinkedHashMap<>();
    private final Map<String, ModuleState> states = new HashMap<>();
    private final Map<String, ModuleError> errors = new LinkedHashMap<>();
    private final List<String> bootWarnings = new ArrayList<>();
    private final List<String> order = new ArrayList<>();
    private final Consumer<String> onToggle;
    private final java.util.function.Supplier<Minecraft> clientSupplier;
    private boolean initialized;

    public ModuleManager() {
        this(id -> { });
    }

    /**
     * @param onToggle receives the module id whenever the user-facing toggle changes, so the owner
     *                 can persist it. It is not called for automatic cascade/failure changes.
     */
    public ModuleManager(Consumer<String> onToggle) {
        this(onToggle, Minecraft::getInstance);
    }

    ModuleManager(Consumer<String> onToggle, java.util.function.Supplier<Minecraft> clientSupplier) {
        this.onToggle = onToggle == null ? id -> { } : onToggle;
        this.clientSupplier = clientSupplier == null ? () -> null : clientSupplier;
    }

    /**
     * Headless registry used by tests and embedding tools. It never resolves a Minecraft client, so
     * lifecycle tests can run without a game instance.
     */
    public static ModuleManager headless(Consumer<String> onToggle) {
        return new ModuleManager(onToggle, () -> null);
    }

    private Minecraft client() {
        try {
            return clientSupplier.get();
        } catch (Throwable throwable) {
            return null;
        }
    }

    public void register(TflModule module) {
        if (module == null) throw new IllegalArgumentException("module == null");
        String id = module.id();
        if (id == null || id.isBlank()) throw new IllegalArgumentException("Module id must not be blank");
        if (!id.equals(id.toLowerCase(java.util.Locale.ROOT))) {
            throw new IllegalArgumentException("Module id must be lower-case: " + id);
        }
        if (modules.containsKey(id)) {
            throw new IllegalStateException("Duplicate TFL Core module id: " + id);
        }
        modules.put(id, module);
        states.put(id, ModuleState.REGISTERED);
    }

    /** Resolves the dependency graph and enables modules according to persisted overrides. */
    public void initialize(Map<String, Boolean> overrides) {
        bootWarnings.clear();
        order.clear();
        Set<String> visiting = new LinkedHashSet<>();
        Set<String> visited = new LinkedHashSet<>();
        for (String id : modules.keySet()) {
            visit(id, visiting, visited);
        }
        initialized = true;

        // Validate dependencies once; a missing dependency disables the dependent.
        for (TflModule module : modules.values()) {
            List<String> missing = new ArrayList<>();
            for (String dependency : module.requires()) {
                if (!modules.containsKey(dependency)) {
                    missing.add(dependency);
                }
            }
            if (!missing.isEmpty()) {
                states.put(module.id(), ModuleState.MISSING_DEPENDENCY);
                warn("Module '" + module.id() + "' needs missing dependencies: " + String.join(", ", missing));
            }
        }

        for (String id : order) {
            TflModule module = modules.get(id);
            if (states.get(id) != ModuleState.REGISTERED) continue;
            boolean desired = overrides == null || !overrides.containsKey(id)
                    ? module.enabledByDefault()
                    : Boolean.TRUE.equals(overrides.get(id));
            if (desired && dependenciesEnabled(module)) {
                enable(module, false);
            } else {
                states.put(id, ModuleState.DISABLED);
            }
        }
    }

    private void visit(String id, Set<String> visiting, Set<String> visited) {
        if (visited.contains(id)) return;
        if (!visiting.add(id)) {
            warn("Dependency cycle detected at module '" + id + "'; affected modules stay disabled.");
            states.put(id, ModuleState.FAILED);
            return;
        }
        TflModule module = modules.get(id);
        if (module != null) {
            for (String dependency : module.requires()) {
                if (modules.containsKey(dependency)) {
                    visit(dependency, visiting, visited);
                }
            }
        }
        visiting.remove(id);
        visited.add(id);
        if (module != null) {
            order.add(id);
        }
    }

    private boolean dependenciesEnabled(TflModule module) {
        for (String dependency : module.requires()) {
            if (!isEnabled(dependency)) return false;
        }
        return true;
    }

    public boolean isRegistered(String id) {
        return modules.containsKey(id);
    }

    public boolean isEnabled(String id) {
        return states.get(id) == ModuleState.ENABLED;
    }

    public ModuleState state(String id) {
        return states.getOrDefault(id, ModuleState.REGISTERED);
    }

    public TflModule module(String id) {
        return modules.get(id);
    }

    public boolean setEnabled(String id, boolean enabled) {
        TflModule module = modules.get(id);
        if (module == null) return false;
        if (!initialized) {
            throw new IllegalStateException("ModuleManager.initialize() must run before toggling modules");
        }
        if (enabled) {
            if (states.get(id) == ModuleState.MISSING_DEPENDENCY) {
                warn("Cannot enable '" + id + "': dependencies are missing.");
                return false;
            }
            // Enabling a feature implies enabling what it needs.
            for (String dependency : module.requires()) {
                if (!isEnabled(dependency) && !setEnabled(dependency, true)) {
                    return false;
                }
            }
            if (!isEnabled(id)) {
                enable(module, true);
            }
            return true;
        }
        disableWithDependents(id, true);
        return true;
    }

    /** Enables every registered module that is currently disabled (used by "enable all"). */
    public void enableAll() {
        for (String id : order) {
            if (states.get(id) == ModuleState.DISABLED) {
                setEnabled(id, true);
            }
        }
    }

    public void resetToDefaults(Map<String, Boolean> overrides) {
        initialized = true;
        for (String id : order) {
            TflModule module = modules.get(id);
            boolean desired = overrides == null || !overrides.containsKey(id)
                    ? module.enabledByDefault()
                    : Boolean.TRUE.equals(overrides.get(id));
            if (desired && dependenciesEnabled(module)) {
                enable(module, false);
            } else if (isEnabled(id)) {
                disableWithDependents(id, false);
            }
        }
    }

    private void enable(TflModule module, boolean userAction) {
        try {
            module.onEnable(client());
            states.put(module.id(), ModuleState.ENABLED);
            errors.remove(module.id());
            if (userAction) onToggle.accept(module.id());
        } catch (Throwable throwable) {
            fail(module.id(), throwable);
        }
    }

    private void disableWithDependents(String id, boolean userAction) {
        // Disable dependents first (reverse topological order) so they can release resources.
        for (int i = order.size() - 1; i >= 0; i--) {
            String candidate = order.get(i);
            if (candidate.equals(id)) continue;
            if (!isEnabled(candidate)) continue;
            TflModule module = modules.get(candidate);
            if (module != null && module.requires().contains(id)) {
                disableWithDependents(candidate, false);
            }
        }
        TflModule module = modules.get(id);
        if (module == null || !isEnabled(id)) return;
        try {
            module.onDisable(client());
        } catch (Throwable throwable) {
            recordError(id, throwable);
            LOGGER.error("TFL Core module '{}' failed while disabling", id, throwable);
        }
        states.put(id, ModuleState.DISABLED);
        if (userAction) onToggle.accept(id);
    }

    public void tick(Minecraft client) {
        if (!initialized) return;
        for (String id : order) {
            if (!isEnabled(id)) continue;
            TflModule module = modules.get(id);
            if (module == null) continue;
            try {
                module.onTick(client);
            } catch (Throwable throwable) {
                fail(id, throwable);
            }
        }
    }

    public void worldJoin(Minecraft client) {
        if (!initialized) return;
        for (String id : order) {
            if (!isEnabled(id)) continue;
            try {
                modules.get(id).onWorldJoin(client);
            } catch (Throwable throwable) {
                fail(id, throwable);
            }
        }
    }

    public void worldLeave(Minecraft client) {
        if (!initialized) return;
        for (String id : order) {
            if (!isEnabled(id)) continue;
            try {
                modules.get(id).onWorldLeave(client);
            } catch (Throwable throwable) {
                fail(id, throwable);
            }
        }
    }

    private void fail(String id, Throwable throwable) {
        recordError(id, throwable);
        LOGGER.error("TFL Core module '{}' failed and has been disabled for this session", id, throwable);
        try {
            TflModule module = modules.get(id);
            if (module != null && isEnabled(id)) {
                module.onDisable(client());
            }
        } catch (Throwable nested) {
            LOGGER.error("TFL Core module '{}' also failed to disable cleanly", id, nested);
        }
        states.put(id, ModuleState.FAILED);
    }

    private void recordError(String id, Throwable throwable) {
        String message = throwable.getClass().getSimpleName()
                + (throwable.getMessage() == null ? "" : ": " + throwable.getMessage());
        errors.put(id, new ModuleError(id, message, System.currentTimeMillis()));
    }

    public void clearFailure(String id) {
        if (states.get(id) == ModuleState.FAILED || states.get(id) == ModuleState.MISSING_DEPENDENCY) {
            states.put(id, ModuleState.DISABLED);
            errors.remove(id);
        }
    }

    public List<ModuleInfo> info() {
        List<ModuleInfo> result = new ArrayList<>();
        for (TflModule module : modules.values()) {
            result.add(new ModuleInfo(module.id(), module.category(), state(module.id()),
                    List.copyOf(module.requires()), module.enabledByDefault(), module.configurable()));
        }
        return result;
    }

    public List<TflModule> enabledModules() {
        List<TflModule> result = new ArrayList<>();
        for (String id : order) {
            if (isEnabled(id)) {
                TflModule module = modules.get(id);
                if (module != null) result.add(module);
            }
        }
        return Collections.unmodifiableList(result);
    }

    public int enabledCount() {
        int count = 0;
        for (ModuleState state : states.values()) {
            if (state == ModuleState.ENABLED) count++;
        }
        return count;
    }

    public int registeredCount() {
        return modules.size();
    }

    public List<ModuleError> errors() {
        return List.copyOf(errors.values());
    }

    public List<String> bootWarnings() {
        return List.copyOf(bootWarnings);
    }

    private void warn(String message) {
        bootWarnings.add(message);
        LOGGER.warn(message);
    }
}
