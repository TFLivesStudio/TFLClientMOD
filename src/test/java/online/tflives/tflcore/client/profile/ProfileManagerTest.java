package online.tflives.tflcore.client.profile;

import net.minecraft.client.Minecraft;
import online.tflives.tflcore.client.config.TflSettings;
import online.tflives.tflcore.client.module.ModuleCategory;
import online.tflives.tflcore.client.module.ModuleManager;
import online.tflives.tflcore.client.module.TflModule;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class ProfileManagerTest {
    private static final List<String> MODULES =
            List.of("crosshair", "toggle-sprint", "zoom", "waypoints");

    private ModuleManager manager() {
        ModuleManager manager = ModuleManager.headless(id -> { });
        for (String id : MODULES) {
            manager.register(new NoopModule(id));
        }
        return manager;
    }

    @Test
    void applyingPvpEnablesItsModulesAndDetectsTheProfile() {
        TflSettings settings = new TflSettings();
        ModuleManager modules = manager();
        modules.initialize(Map.of());

        ProfileManager.apply(ClientProfile.PVP, settings, modules);

        for (String id : ClientProfile.PVP.modules()) {
            assertTrue(modules.isEnabled(id), id + " must be enabled by the PvP profile");
        }
        assertEquals(ClientProfile.PVP, ProfileManager.detect(modules));
        assertEquals("CUSTOM", settings.perf.preset);
        assertFalse(settings.ui.animatedBackground);
        assertTrue(settings.ui.reducedMotion);
    }

    @Test
    void applyingSurvivalDisablesPvpOnlyModules() {
        TflSettings settings = new TflSettings();
        ModuleManager modules = manager();
        modules.initialize(Map.of());
        ProfileManager.apply(ClientProfile.PVP, settings, modules);

        ProfileManager.apply(ClientProfile.SURVIVAL, settings, modules);

        assertTrue(modules.isEnabled("zoom"));
        assertTrue(modules.isEnabled("waypoints"));
        assertFalse(modules.isEnabled("crosshair"));
        assertFalse(modules.isEnabled("toggle-sprint"));
        assertEquals(ClientProfile.SURVIVAL, ProfileManager.detect(modules));
        assertTrue(settings.ui.animatedBackground);
    }

    @Test
    void applyingMinimalDisablesEverythingAndIsDetected() {
        TflSettings settings = new TflSettings();
        ModuleManager modules = manager();
        modules.initialize(Map.of());

        ProfileManager.apply(ClientProfile.MINIMAL, settings, modules);

        assertEquals(0, modules.enabledCount());
        assertEquals(ClientProfile.MINIMAL, ProfileManager.detect(modules));
        assertFalse(settings.ui.animatedBackground);
        assertTrue(settings.ui.reducedMotion);
    }

    @Test
    void nonMatchingStateIsReportedAsCustom() {
        ModuleManager modules = manager();
        modules.initialize(Map.of(
                "crosshair", false,
                "toggle-sprint", false,
                "zoom", false,
                "waypoints", false));
        modules.setEnabled("zoom", true);
        modules.setEnabled("crosshair", true);

        assertEquals(ClientProfile.CUSTOM, ProfileManager.detect(modules));
    }

    @Test
    void customProfileNeverChangesState() {
        TflSettings settings = new TflSettings();
        ModuleManager modules = manager();
        modules.initialize(Map.of("zoom", false, "waypoints", false));
        settings.ui.animatedBackground = false;

        ProfileManager.apply(ClientProfile.CUSTOM, settings, modules);

        assertFalse(modules.isEnabled("zoom"));
        assertFalse(modules.isEnabled("waypoints"));
        assertFalse(settings.ui.animatedBackground);
    }

    @Test
    void modulesOutsideTheProfileAreDisabledButUnknownIdsAreIgnored() {
        TflSettings settings = new TflSettings();
        ModuleManager modules = manager();
        modules.register(new NoopModule("experimental"));
        modules.initialize(Map.of("experimental", true));

        ProfileManager.apply(ClientProfile.BUILDER, settings, modules);

        assertTrue(modules.isEnabled("waypoints"));
        assertFalse(modules.isEnabled("experimental"));
        assertEquals(ClientProfile.BUILDER, ProfileManager.detect(modules));
    }

    @Test
    void nextSkipsTheAutomaticCustomFallback() {
        assertEquals(ClientProfile.SURVIVAL, ClientProfile.PVP.next());
        assertEquals(ClientProfile.MINIMAL, ClientProfile.EXPLORATION.next());
        assertEquals(ClientProfile.PVP, ClientProfile.MINIMAL.next());
        assertEquals(ClientProfile.CUSTOM, ClientProfile.fromName("does-not-exist"));
        assertEquals(ClientProfile.PVP, ClientProfile.fromName("pvp"));
    }

    private record NoopModule(String id) implements TflModule {
        @Override
        public ModuleCategory category() {
            return ModuleCategory.CORE;
        }

        @Override
        public void onEnable(Minecraft client) {
        }
    }
}
