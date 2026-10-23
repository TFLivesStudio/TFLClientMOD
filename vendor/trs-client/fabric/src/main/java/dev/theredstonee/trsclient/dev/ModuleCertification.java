package dev.theredstonee.trsclient.dev;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import dev.theredstonee.trsclient.TrsClient;
import dev.theredstonee.trsclient.compat.Mc;
import dev.theredstonee.trsclient.core.config.ConfigStore;
import dev.theredstonee.trsclient.core.config.TrsConfig;
import dev.theredstonee.trsclient.core.module.Module;
import dev.theredstonee.trsclient.core.module.TrsModules;
import dev.theredstonee.trsclient.core.waypoint.Waypoint;
import dev.theredstonee.trsclient.core.waypoint.WaypointStore;
import dev.theredstonee.trsclient.screen.CrosshairEditorScreen;
import dev.theredstonee.trsclient.screen.HudEditorScreen;
import dev.theredstonee.trsclient.screen.TrsMenuScreen;
import dev.theredstonee.trsclient.screen.WaypointListScreen;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;

import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

/** Opt-in native smoke. A successful toggle NEVER certifies a module's gameplay behavior. */
public final class ModuleCertification {
	private static final Gson GSON = new GsonBuilder().setPrettyPrinting().serializeNulls().create();
	private final Minecraft mc;
	private final Path output;
	private final Map<String, Object> report = new LinkedHashMap<>();
	private final List<Map<String, Object>> rows = new ArrayList<>();
	private final List<Map<String, Object>> checks = new ArrayList<>();
	private final AtomicBoolean finishing = new AtomicBoolean();
	private volatile boolean done;
	private TrsConfig before;
	private boolean pauseBefore;
	private Waypoint waypoint;
	private int stage, index, wait, screenIndex;
	private Module current;
	private Map<String, Object> row;
	private boolean failed;
	private final long started = System.currentTimeMillis();

	private ModuleCertification(Minecraft mc) {
		this.mc = mc;
		output = mc.gameDirectory.toPath().resolve("module-certification-" + AutoTest.MC_VERSION + ".json");
		report.put("schemaVersion", 1);
		report.put("minecraft", AutoTest.MC_VERSION);
		report.put("evidenceKind", "native-fabric-development-client");
		report.put("scope", "state-toggle smoke, rendered frames, selected screen lifecycle, disk config roundtrip and waypoint CRUD; not gameplay certification");
		report.put("fullyCertifiedModules", 0);
		report.put("startedEpochMs", started);
		report.put("status", "running");
		report.put("modules", rows);
		report.put("checks", checks);
	}

	public static void install() {
		ModuleCertification test = new ModuleCertification(Minecraft.getInstance());
		test.persist();
		ClientTickEvents.END_CLIENT_TICK.register(test::tick);
		// Wall-clock watchdog also works if world loading or a renderer stops delivering ticks.
		Thread watchdog = new Thread(() -> {
			try { Thread.sleep(240000); } catch (InterruptedException e) { return; }
			if (test.done) return;
			test.record("wallClockTimeout", false, "240 seconds elapsed; requesting normal client shutdown");
			test.reportStatus("timeout");
			test.mc.execute(() -> test.finish("timeout"));
			try { Thread.sleep(30000); } catch (InterruptedException e) { return; }
			if (!test.done) {
				test.reportStatus("timeout-hard-stop-no-restoration-guarantee");
				// Dev-only last resort: an unresponsive game cannot perform a normal shutdown.
				Runtime.getRuntime().halt(2);
			}
		}, "TFL-module-certification-watchdog");
		watchdog.setDaemon(true);
		watchdog.start();
	}

	private void tick(Minecraft ignored) {
		if (done || finishing.get()) return;
		try {
			if (wait-- > 0) return;
			TrsModules m = TrsClient.get().modules();
			switch (stage) {
				case 0:
					if (Mc.overlay() != null || Mc.screen() == null) return;
					before = m.registry.capture();
                    m.dynamicFps.setEnabled(false);
					pauseBefore = mc.options.pauseOnLostFocus;
					mc.options.pauseOnLostFocus = false;
					Mc.setScreen(null);
					AutoTest.startWorld(mc, "tfl-module-certification-" + AutoTest.MC_VERSION, 764105L);
					stage = 1;
					break;
				case 1:
					if (mc.level == null || mc.player == null) return;
					Mc.setScreen(null);
					record("worldLoaded", true, "integrated world and player present");
					stage = 2;
					wait = 20;
					break;
				case 2:
					if (index >= m.registry.all().size()) { stage = 5; break; }
					current = m.registry.all().get(index++);
					row = new LinkedHashMap<>();
					row.put("id", current.id());
					row.put("available", current.available());
					row.put("fullyCertified", false);
					row.put("gameplay", "untested: activating state does not exercise all hooks or input actions");
					synchronized (this) { rows.add(row); }
					if (!current.userToggleable()) {
						row.put("status", "fixed-infrastructure-settings-only");
						row.put("activation", "always active by design; state toggling skipped");
						persist(); break;
					}
					if (!current.available() || "trsOnline".equals(current.id()) || "social".equals(current.id()) || "clips".equals(current.id())) {
						row.put("status", "skipped-unavailable-or-disabled-service"); persist(); break;
					}
					current.setEnabled(true);
					row.put("activation", current.isEnabled() ? "state-pass" : "fail");
					if (!current.isEnabled()) failed = true;
					stage = 3; wait = 8;
					break;
				case 3:
					row.put("enabledFrames", "survived-8-client-ticks; visual correctness unasserted");
					current.setEnabled(false);
					row.put("deactivation", !current.isEnabled() ? "state-pass" : "fail");
					if (current.isEnabled()) failed = true;
					stage = 4; wait = 8;
					break;
				case 4:
					current.read(before.modules.get(current.id()));
					boolean restored = GSON.toJsonTree(current.write()).equals(GSON.toJsonTree(before.modules.get(current.id())));
					row.put("restoration", restored ? "pass" : "fail");
					row.put("status", restored ? "state-smoke-complete" : "fail");
					if (!restored) failed = true;
					m.dynamicFps.setEnabled(false); // keep the unfocused CI window responsive between module checks
                    persist(); stage = 2;
					break;
				case 5:
					// Separate scratch file: never replace the user's config or ConfigStore.active().
					Path scratch = output.resolveSibling("module-certification-config.json");
					TrsConfig snapshot = m.registry.capture();
					new ConfigStore(scratch).save(m.registry);
					TrsConfig decoded;
					try (Reader reader = Files.newBufferedReader(scratch, StandardCharsets.UTF_8)) {
						decoded = GSON.fromJson(reader, TrsConfig.class);
					}
					m.registry.apply(decoded);
					record("configDiskRoundtrip", GSON.toJsonTree(snapshot).equals(GSON.toJsonTree(m.registry.capture())),
							"ConfigStore.save -> file -> Gson decode -> registry.apply; restart not tested");
					waypoint = TrsClient.get().waypoints().create("cert-" + Long.toHexString(started), 0x44CCAA);
					if (waypoint == null) throw new IllegalStateException("Waypoint creation returned null");
					WaypointStore reloaded = new WaypointStore(TrsClient.get().waypoints().store().file());
					reloaded.load();
					record("waypointDiskCreate", reloaded.all(TrsClient.get().waypoints().worldKey()).stream()
							.anyMatch(w -> waypoint.name.equals(w.name)), "unique temporary waypoint name found after independent file load");
					stage = 6;
					break;
				case 6:
					if (screenIndex >= 4) { stage = 8; break; }
					Mc.setScreen(screen(screenIndex));
					stage = 7; wait = 15;
					break;
				case 7:
					Class<?> expected = screen(screenIndex).getClass();
					record(expected.getSimpleName(), Mc.screen() != null && expected.isInstance(Mc.screen()),
							"screen survived 15 ticks; screenshot requested; input/visual QA remains manual");
					AutoTest.shot(mc, "trsclient-certification-" + expected.getSimpleName());
					Mc.setScreen(null); screenIndex++; stage = 6; wait = 4;
					break;
				case 8: finish(failed ? "failed" : "smoke-complete"); break;
				default: throw new IllegalStateException("Unknown stage " + stage);
			}
		} catch (Throwable error) {
			record("runnerException", false, error.toString());
			if (row != null) { row.put("error", error.toString()); row.put("status", "failed"); }
			finish("failed");
		}
	}

	private Screen screen(int i) {
		switch (i) {
			case 0: return new TrsMenuScreen(null);
			case 1: return new HudEditorScreen(null).selectFirst();
			case 2: return new CrosshairEditorScreen(null);
			default: return new WaypointListScreen(null);
		}
	}

	private void finish(String status) {
		if (!finishing.compareAndSet(false, true)) return;
		try {
			Mc.setScreen(null);
			if (waypoint != null) {
				String name = waypoint.name;
				TrsClient.get().waypoints().remove(waypoint);
				WaypointStore reloaded = new WaypointStore(TrsClient.get().waypoints().store().file());
				reloaded.load();
				record("waypointDiskRemove", reloaded.all(TrsClient.get().waypoints().worldKey()).stream()
						.noneMatch(w -> name.equals(w.name)), "temporary waypoint absent after independent disk load");
			}
			if (before != null) {
				TrsClient.get().modules().registry.apply(before);
				record("finalRegistryRestore", GSON.toJsonTree(before).equals(GSON.toJsonTree(TrsClient.get().modules().registry.capture())), "all registry and config parts restored");
				TrsClient.get().saveConfig();
				mc.options.pauseOnLostFocus = pauseBefore;
			}
			AutoTest.disconnect(mc);
		} catch (Throwable error) { record("cleanup", false, error.toString()); }
		reportStatus(failed && "smoke-complete".equals(status) ? "failed" : status);
		mc.stop();
		done = true;
	}

	private synchronized void record(String name, boolean passed, String detail) {
		Map<String, Object> check = new LinkedHashMap<>();
		check.put("name", name); check.put("passed", passed); check.put("detail", detail);
		checks.add(check);
		if (!passed) failed = true;
		persist();
	}

	private synchronized void reportStatus(String status) {
		report.put("status", status);
		report.put("elapsedMs", System.currentTimeMillis() - started);
		persist();
	}

	private synchronized void persist() {
		try {
			Path tmp = output.resolveSibling(output.getFileName() + ".tmp");
			Files.write(tmp, GSON.toJson(report).getBytes(StandardCharsets.UTF_8));
			Files.move(tmp, output, StandardCopyOption.REPLACE_EXISTING);
		} catch (Exception error) { TrsClient.LOGGER.error("[ModuleCertification] Cannot persist report", error); }
	}
}
