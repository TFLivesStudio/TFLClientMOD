package dev.theredstonee.trsclient.core.config;

import com.google.gson.Gson;
import dev.theredstonee.trsclient.core.module.Module;
import dev.theredstonee.trsclient.core.module.TrsModules;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/** Registry/config regressions only: these tests make no native gameplay claim. */
class ModuleCertificationRegressionTest {
	@TempDir Path dir;
	private static final Gson GSON = new Gson();

	@Test void everyRegisteredModuleRetainsItsFullConfigAcrossToggleRestoreAndDiskLoad() throws Exception {
		TrsModules source = new TrsModules();
		// Non-default values make this more than a default-config serialization test.
		source.zoomFactor.set(7.5);
		source.fps.position().offsetX = 0.217;
		source.fps.textColor.set(0x13579B);
		TrsConfig initial = source.registry.capture();
		Set<String> ids = new HashSet<>();
		for (Module module : source.registry.all()) {
			assertTrue(ids.add(module.id()), "duplicate module ID " + module.id());
			if (!module.userToggleable()) {
				assertTrue(module.isEnabled(), "fixed infrastructure must remain enabled");
				continue;
			}
			module.setEnabled(!module.isEnabled());
			module.read(initial.modules.get(module.id()));
			assertEquals(GSON.toJsonTree(initial.modules.get(module.id())), GSON.toJsonTree(module.write()), module.id());
			// Persist the opposite of defaults for every module, including unavailable ones.
			module.setEnabled(!module.isEnabled());
		}
		assertEquals(79, ids.size(), "Update the release module audit when changing the registry");
		Path file = dir.resolve("roundtrip.json");
		new ConfigStore(file).save(source.registry);
		TrsModules loaded = new TrsModules();
		assertEquals(ConfigStore.Status.LOADED, new ConfigStore(file).load(loaded.registry));
		assertEquals(GSON.toJsonTree(source.registry.capture()), GSON.toJsonTree(loaded.registry.capture()));
	}

	@Test void unavailableOnlineModuleStillRoundtripsWithoutBecomingAvailable() throws Exception {
		TrsModules source = new TrsModules();
		assertFalse(source.trsOnline.available());
		source.trsOnline.setEnabled(true); // availability must remain independent from saved user state
		Path file = dir.resolve("offline.json");
		new ConfigStore(file).save(source.registry);
		TrsModules loaded = new TrsModules();
		new ConfigStore(file).load(loaded.registry);
		assertFalse(loaded.trsOnline.available());
		assertTrue(loaded.trsOnline.isEnabled());
	}
}
