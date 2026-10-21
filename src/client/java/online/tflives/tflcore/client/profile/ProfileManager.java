package online.tflives.tflcore.client.profile;

import online.tflives.tflcore.client.config.TflSettings;
import online.tflives.tflcore.client.module.ModuleManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.LinkedHashSet;
import java.util.Set;

/** Applies and detects gameplay profiles without touching external mods or vanilla options. */
public final class ProfileManager {
    private static final Logger LOGGER = LoggerFactory.getLogger("tflcore");

    private ProfileManager() {
    }

    public static void apply(ClientProfile profile, TflSettings settings, ModuleManager modules) {
        if (profile == null || !profile.isAutomatic()) {
            return;
        }
        switch (profile) {
            case MINIMAL -> {
                settings.ui.animatedBackground = false;
                settings.ui.reducedMotion = true;
            }
            case PVP -> {
                settings.ui.animatedBackground = false;
                settings.ui.reducedMotion = true;
            }
            default -> {
                settings.ui.animatedBackground = true;
                settings.ui.reducedMotion = false;
            }
        }
        Set<String> wanted = profile.modules();
        // Enable first so dependencies exist, then disable anything outside the profile.
        for (String id : wanted) {
            if (modules.isRegistered(id)) {
                modules.setEnabled(id, true);
            }
        }
        for (ModuleManager.ModuleInfo info : modules.info()) {
            if (!wanted.contains(info.id()) && modules.isEnabled(info.id())) {
                modules.setEnabled(info.id(), false);
            }
        }
        settings.markCustom();
        LOGGER.info("Applied TFL Core profile {} ({} modules)", profile.name(), modules.enabledCount());
    }

    /** Returns the profile whose module set matches the current state, or CUSTOM. */
    public static ClientProfile detect(ModuleManager modules) {
        for (ClientProfile profile : ClientProfile.values()) {
            if (!profile.isAutomatic()) continue;
            if (matches(profile, modules)) {
                return profile;
            }
        }
        return ClientProfile.CUSTOM;
    }

    private static boolean matches(ClientProfile profile, ModuleManager modules) {
        Set<String> wanted = profile.modules();
        Set<String> current = new LinkedHashSet<>();
        Set<String> registered = new LinkedHashSet<>();
        for (ModuleManager.ModuleInfo info : modules.info()) {
            registered.add(info.id());
            if (modules.isEnabled(info.id())) {
                current.add(info.id());
            }
        }
        // Compare only modules that are registered in this build; unknown ids are ignored.
        Set<String> expected = new LinkedHashSet<>(wanted);
        expected.retainAll(registered);
        return current.equals(expected);
    }
}
