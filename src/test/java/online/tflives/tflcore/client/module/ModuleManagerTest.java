package online.tflives.tflcore.client.module;

import net.minecraft.client.Minecraft;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class ModuleManagerTest {
    private final List<String> log = new ArrayList<>();
    private final List<String> toggles = new ArrayList<>();

    private ModuleManager manager() {
        return new ModuleManager(toggles::add, () -> null);
    }

    private TestModule module(String id) {
        return new TestModule(id, List.of(), true, log);
    }

    private TestModule module(String id, boolean defaultEnabled, String... requires) {
        return new TestModule(id, List.of(requires), defaultEnabled, log);
    }

    @Test
    void duplicateIdsAreRejected() {
        ModuleManager manager = manager();
        manager.register(module("a"));
        assertThrows(IllegalStateException.class, () -> manager.register(module("a")));
    }

    @Test
    void invalidIdsAreRejected() {
        ModuleManager manager = manager();
        assertThrows(IllegalArgumentException.class, () -> manager.register(module("")));
        assertThrows(IllegalArgumentException.class, () -> manager.register(module("NotLower")));
    }

    @Test
    void dependenciesAreEnabledFirst() {
        ModuleManager manager = manager();
        TestModule base = module("base");
        TestModule dependent = module("dependent", true, "base");
        manager.register(dependent);
        manager.register(base);
        manager.initialize(Map.of());

        assertTrue(manager.isEnabled("base"));
        assertTrue(manager.isEnabled("dependent"));
        int baseIndex = log.indexOf("enable:base");
        int dependentIndex = log.indexOf("enable:dependent");
        assertTrue(baseIndex >= 0);
        assertTrue(dependentIndex > baseIndex, "dependencies must be enabled before dependents");
    }

    @Test
    void missingDependencyKeepsModuleDisabledAndWarns() {
        ModuleManager manager = manager();
        manager.register(module("needs-ghost", true, "ghost"));
        manager.initialize(Map.of());

        assertFalse(manager.isEnabled("needs-ghost"));
        assertEquals(ModuleState.MISSING_DEPENDENCY, manager.state("needs-ghost"));
        assertFalse(manager.bootWarnings().isEmpty());
    }

    @Test
    void cyclesAreDetectedAndStayDisabled() {
        ModuleManager manager = manager();
        manager.register(module("a", true, "b"));
        manager.register(module("b", true, "a"));
        manager.initialize(Map.of());

        assertFalse(manager.isEnabled("a"));
        assertFalse(manager.isEnabled("b"));
        assertTrue(manager.bootWarnings().stream().anyMatch(warning -> warning.contains("cycle")));
    }

    @Test
    void disablingADependencyCascadesToDependents() {
        ModuleManager manager = manager();
        manager.register(module("a"));
        manager.register(module("b", true, "a"));
        manager.register(module("c", true, "b"));
        manager.initialize(Map.of());
        assertTrue(manager.isEnabled("c"));
        log.clear();

        assertTrue(manager.setEnabled("a", false));
        assertFalse(manager.isEnabled("a"));
        assertFalse(manager.isEnabled("b"));
        assertFalse(manager.isEnabled("c"));
        assertTrue(log.indexOf("disable:c") < log.indexOf("disable:b"));
        assertTrue(log.indexOf("disable:b") < log.indexOf("disable:a"));
    }

    @Test
    void enablingADependentEnablesItsDependencies() {
        ModuleManager manager = manager();
        manager.register(module("base", false));
        manager.register(module("dependent", true, "base"));
        manager.initialize(Map.of("base", false, "dependent", true));
        assertFalse(manager.isEnabled("base"));
        assertFalse(manager.isEnabled("dependent"));

        assertTrue(manager.setEnabled("dependent", true));
        assertTrue(manager.isEnabled("base"));
        assertTrue(manager.isEnabled("dependent"));
    }

    @Test
    void aThrowingModuleIsDisabledWithoutBreakingOthers() {
        ModuleManager manager = manager();
        TestModule broken = module("broken");
        broken.failTick = true;
        TestModule healthy = module("healthy");
        manager.register(broken);
        manager.register(healthy);
        manager.initialize(Map.of());

        manager.tick(null);

        assertFalse(manager.isEnabled("broken"));
        assertEquals(ModuleState.FAILED, manager.state("broken"));
        assertTrue(manager.isEnabled("healthy"));
        assertEquals(1, manager.errors().size());
        assertTrue(manager.errors().get(0).shortMessage().contains("boom"));

        log.clear();
        manager.tick(null);
        assertFalse(log.contains("tick:broken"), "failed modules must not run again");
        assertTrue(log.contains("tick:healthy"));
    }

    @Test
    void aThrowingOnEnableIsIsolated() {
        ModuleManager manager = manager();
        TestModule broken = module("broken");
        broken.failEnable = true;
        manager.register(broken);
        manager.register(module("healthy"));
        manager.initialize(Map.of());

        assertEquals(ModuleState.FAILED, manager.state("broken"));
        assertTrue(manager.isEnabled("healthy"));
        assertFalse(manager.errors().isEmpty());
    }

    @Test
    void userTogglesArePersistedButCascadesAreNotReportedAsUserActions() {
        ModuleManager manager = manager();
        manager.register(module("a"));
        manager.register(module("b", true, "a"));
        manager.initialize(Map.of());
        toggles.clear();

        manager.setEnabled("a", false);
        assertEquals(List.of("a"), toggles,
                "only the module the user clicked is reported for persistence");

        toggles.clear();
        manager.setEnabled("b", true);
        // Enabling b auto-enables a, and both changes are user-visible persistence events.
        assertTrue(toggles.contains("a"));
        assertTrue(toggles.contains("b"));
    }

    @Test
    void resetToDefaultsRestoresDeclaredDefaults() {
        ModuleManager manager = manager();
        manager.register(module("on", true));
        manager.register(module("off", false));
        manager.initialize(Map.of("on", false, "off", true));
        assertFalse(manager.isEnabled("on"));
        assertTrue(manager.isEnabled("off"));

        manager.resetToDefaults(Map.of());
        assertTrue(manager.isEnabled("on"));
        assertFalse(manager.isEnabled("off"));
    }

    @Test
    void enableAllOnlyTouchesRegisteredModules() {
        ModuleManager manager = manager();
        manager.register(module("a", false));
        manager.register(module("b", false));
        manager.initialize(Map.of("a", false, "b", false));
        assertEquals(0, manager.enabledCount());

        manager.enableAll();
        assertEquals(2, manager.enabledCount());
        assertEquals(2, manager.registeredCount());
    }

    @Test
    void infoExposesCategoryAndDependencies() {
        ModuleManager manager = manager();
        manager.register(module("base"));
        manager.register(module("dependent", true, "base"));
        manager.initialize(Map.of());

        ModuleManager.ModuleInfo info = manager.info().stream()
                .filter(candidate -> candidate.id().equals("dependent"))
                .findFirst()
                .orElseThrow();
        assertEquals(ModuleCategory.CORE, info.category());
        assertEquals(List.of("base"), info.requires());
        assertTrue(info.enabledByDefault());
        assertTrue(info.configurable());
    }

    private static final class TestModule implements TflModule {
        private final String id;
        private final List<String> requires;
        private final boolean defaultEnabled;
        private final List<String> log;
        boolean failEnable;
        boolean failTick;

        private TestModule(String id, List<String> requires, boolean defaultEnabled, List<String> log) {
            this.id = id;
            this.requires = requires;
            this.defaultEnabled = defaultEnabled;
            this.log = log;
        }

        @Override
        public String id() {
            return id;
        }

        @Override
        public ModuleCategory category() {
            return ModuleCategory.CORE;
        }

        @Override
        public List<String> requires() {
            return requires;
        }

        @Override
        public boolean enabledByDefault() {
            return defaultEnabled;
        }

        @Override
        public void onEnable(Minecraft client) {
            log.add("enable:" + id);
            if (failEnable) {
                throw new IllegalStateException("enable boom " + id);
            }
        }

        @Override
        public void onDisable(Minecraft client) {
            log.add("disable:" + id);
        }

        @Override
        public void onTick(Minecraft client) {
            log.add("tick:" + id);
            if (failTick) {
                failTick = false;
                throw new IllegalStateException("boom " + id);
            }
        }
    }
}
