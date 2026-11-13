package online.tflives.tflcore.client;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.screen.v1.ScreenEvents;
import net.fabricmc.fabric.api.client.screen.v1.Screens;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.PauseScreen;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.network.chat.Component;
import online.tflives.tflcore.client.command.TflCommands;
import online.tflives.tflcore.client.config.SettingsStore;
import online.tflives.tflcore.client.config.TflSettings;
import online.tflives.tflcore.client.hud.Benchmark;
import online.tflives.tflcore.client.hud.HudManager;
import online.tflives.tflcore.client.hud.widget.ArmorWidget;
import online.tflives.tflcore.client.hud.widget.BiomeWidget;
import online.tflives.tflcore.client.hud.widget.ChunkWidget;
import online.tflives.tflcore.client.hud.widget.ClockWidget;
import online.tflives.tflcore.client.hud.widget.CoordinatesWidget;
import online.tflives.tflcore.client.hud.widget.CpsWidget;
import online.tflives.tflcore.client.hud.widget.DirectionWidget;
import online.tflives.tflcore.client.hud.widget.FpsWidget;
import online.tflives.tflcore.client.hud.widget.FrametimeWidget;
import online.tflives.tflcore.client.hud.widget.HeldItemWidget;
import online.tflives.tflcore.client.hud.widget.KeystrokesWidget;
import online.tflives.tflcore.client.hud.widget.PerformanceWidget;
import online.tflives.tflcore.client.hud.widget.PingWidget;
import online.tflives.tflcore.client.hud.widget.PotionsWidget;
import online.tflives.tflcore.client.hud.widget.SpeedWidget;
import online.tflives.tflcore.client.hud.widget.SprintWidget;
import online.tflives.tflcore.client.hud.widget.TargetWidget;
import online.tflives.tflcore.client.hud.widget.WaypointWidget;
import online.tflives.tflcore.client.hud.widget.WorldTimeWidget;
import online.tflives.tflcore.client.keybind.KeybindManager;
import online.tflives.tflcore.client.module.ModuleManager;
import online.tflives.tflcore.client.profile.ClientProfile;
import online.tflives.tflcore.client.profile.ProfileManager;
import online.tflives.tflcore.client.pvp.CrosshairModule;
import online.tflives.tflcore.client.pvp.ToggleSprintModule;
import online.tflives.tflcore.client.survival.WaypointModule;
import online.tflives.tflcore.client.survival.ZoomModule;
import online.tflives.tflcore.client.ui.TflDiagnosticsScreen;
import online.tflives.tflcore.client.ui.TflHudEditorScreen;
import online.tflives.tflcore.client.ui.TflMainMenuScreen;
import online.tflives.tflcore.client.ui.TflMotion;
import online.tflives.tflcore.client.ui.TflSettingsScreen;
import online.tflives.tflcore.client.ui.TflTheme;
import online.tflives.tflcore.client.ui.UiClock;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Objects;

/**
 * Client-only bootstrap for TFL Core / TFLClient.
 *
 * <p>Initialization order is deliberate: settings are loaded and validated first, then the HUD and
 * keybinds are registered, then modules are resolved against persisted overrides. Nothing in this
 * class performs network access, telemetry or remote code loading.</p>
 */
public final class TflCoreClient implements ClientModInitializer {
    public static final String MOD_ID = "tflcore";
    private static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    private static SettingsStore settings;
    private static ModuleManager modules;
    private static HudManager hud;
    private static KeybindManager keybinds;
    private static WaypointModule waypoints;
    private static ClientProfile profile = ClientProfile.CUSTOM;

    private static TflTheme cachedTheme;
    private static String cachedThemeId;
    private static String cachedAccent;
    private static boolean cachedContrast;
    private static boolean cachedTransparency;

    private boolean inWorld;
    private boolean noticeShown;

    public static SettingsStore settings() {
        if (settings == null) throw new IllegalStateException("TFL Core has not initialized yet");
        return settings;
    }

    public static ModuleManager modules() {
        return modules;
    }

    public static HudManager hud() {
        return hud;
    }

    public static KeybindManager keybinds() {
        return keybinds;
    }

    public static WaypointModule waypoints() {
        return waypoints;
    }

    public static ClientProfile profile() {
        return profile;
    }

    /** Cached theme tokens; safe to call every frame from widgets and screens. */
    public static TflTheme theme() {
        TflSettings current = settings().get();
        if (cachedTheme == null
                || !Objects.equals(cachedThemeId, current.ui.theme)
                || !Objects.equals(cachedAccent, current.ui.accent)
                || cachedContrast != current.ui.highContrast
                || cachedTransparency != current.ui.reducedTransparency) {
            cachedThemeId = current.ui.theme;
            cachedAccent = current.ui.accent;
            cachedContrast = current.ui.highContrast;
            cachedTransparency = current.ui.reducedTransparency;
            cachedTheme = TflTheme.from(current);
            if (current.ui.highContrast) {
                cachedTheme = cachedTheme.withText(0xFFFFFFFF, 0xFFD6D9E0);
            }
        }
        return cachedTheme;
    }

    public static void applyProfile(ClientProfile next) {
        if (next == null) return;
        ProfileManager.apply(next, settings.get(), modules);
        if (next.isAutomatic()) {
            hud.applyWidgetSet(next.widgets());
            profile = ProfileManager.detect(modules);
        } else {
            profile = ClientProfile.CUSTOM;
        }
        settings.requestSave();
    }

    public static void notifyPlayer(Minecraft client, Component message) {
        if (client == null) return;
        try {
            client.gui.chatListener().handleSystemMessage(
                    Component.literal("[TFL] ").append(message), false);
        } catch (RuntimeException ignored) {
            // Chat is unavailable in some screens; the log still contains the event.
        }
    }

    @Override
    public void onInitializeClient() {
        settings = new SettingsStore(FabricLoader.getInstance().getConfigDir().resolve("tflcore.json"));
        settings.get().normalize();

        keybinds = new KeybindManager();
        keybinds.register();

        hud = new HudManager(settings);
        registerHudWidgets();
        hud.initialize();
        hud.attach();

        modules = new ModuleManager(id -> {
            settings.get().setModuleEnabled(id, modules.isEnabled(id));
            settings.requestSave();
        });
        modules.register(new CrosshairModule());
        modules.register(new ToggleSprintModule(settings));
        modules.register(new ZoomModule(keybinds, settings));
        waypoints = new WaypointModule(settings);
        modules.register(waypoints);
        // 0.2 stored feature flags; seed module overrides once so upgrades keep the user's choice.
        settings.get().modules.putIfAbsent("zoom", settings.get().survival.zoom);
        settings.get().modules.putIfAbsent("toggle-sprint", settings.get().pvp.toggleSprint);
        modules.initialize(settings.get().modules);
        profile = ProfileManager.detect(modules);

        ClientTickEvents.END_CLIENT_TICK.register(this::tick);
        ClientLifecycleEvents.CLIENT_STOPPING.register(client -> {
            hud.setEditorActive(false);
            // Restore temporary option changes (zoom FOV, sprint key) before the client exits.
            modules.worldLeave(client);
            settings.flush();
        });
        ScreenEvents.AFTER_INIT.register(this::afterScreenInit);
        ClientCommandRegistrationCallback.EVENT.register((dispatcher, registryAccess) ->
                TflCommands.register(dispatcher));

        LOGGER.info("TFL Core {} initialized. {} modules registered, {} enabled, {} HUD widgets, {} waypoints.",
                BuildInfo.VERSION, modules.registeredCount(), modules.enabledCount(),
                hud.widgets().size(), settings.get().survival.waypoints.size());
        if (settings.isReadOnly()) {
            LOGGER.warn("TFL Core settings are read-only (newer schema or oversized file).");
        }
        if (settings.wasMigrated()) {
            LOGGER.info("TFL Core settings migrated to schema {}; backups in {}.",
                    TflSettings.SCHEMA_VERSION, settings.backupDir());
        }
        if (settings.isCorrupt()) {
            LOGGER.warn("TFL Core settings were unreadable; defaults are active and the original was backed up.");
        }
    }

    private void registerHudWidgets() {
        hud.register(new FpsWidget());
        hud.register(new FrametimeWidget());
        hud.register(new CoordinatesWidget());
        hud.register(new DirectionWidget());
        hud.register(new BiomeWidget());
        hud.register(new ClockWidget());
        hud.register(new WorldTimeWidget());
        hud.register(new CpsWidget());
        hud.register(new KeystrokesWidget());
        hud.register(new PingWidget());
        hud.register(new SprintWidget());
        hud.register(new SpeedWidget());
        hud.register(new ArmorWidget());
        hud.register(new PotionsWidget());
        hud.register(new HeldItemWidget());
        hud.register(new TargetWidget());
        hud.register(new ChunkWidget());
        hud.register(new WaypointWidget());
        hud.register(new PerformanceWidget());
    }

    private void tick(Minecraft client) {
        UiClock.tick();
        TflSettings current = settings.get();
        TflMotion.configure(current.ui.transitionMillis, current.ui.reducedMotion);
        settings.tick();
        Benchmark.tick(client);
        modules.tick(client);

        boolean nowInWorld = client.level != null && client.player != null;
        if (nowInWorld && !inWorld) {
            inWorld = true;
            modules.worldJoin(client);
        } else if (!nowInWorld && inWorld) {
            inWorld = false;
            modules.worldLeave(client);
        }

        handleKeybinds(client);
        showStartupNotice(client);
    }

    private void handleKeybinds(Minecraft client) {
        // Key mappings stay pressed while a screen is open; never trigger gameplay actions there.
        boolean noScreen = client.gui.screen() == null;
        while (keybinds.consume(KeybindManager.Action.SETTINGS)) {
            if (noScreen && !(client.gui.screen() instanceof TflSettingsScreen)) {
                client.gui.setScreen(new TflSettingsScreen(client.gui.screen()));
            }
        }
        if (keybinds.consume(KeybindManager.Action.HUD_EDITOR) && noScreen) {
            if (inWorld) {
                client.gui.setScreen(new TflHudEditorScreen(client.gui.screen()));
            } else {
                notifyPlayer(client, Component.translatable("tflcore.notice.world_required"));
            }
        }
        if (keybinds.consume(KeybindManager.Action.WAYPOINT) && noScreen) {
            if (inWorld && waypoints.addAtPlayer(client, Component.translatable("tflcore.waypoint.default_name").getString())) {
                notifyPlayer(client, Component.translatable("tflcore.notice.waypoint_added"));
            } else if (!inWorld) {
                notifyPlayer(client, Component.translatable("tflcore.notice.world_required"));
            }
        }
        if (keybinds.consume(KeybindManager.Action.DIAGNOSTICS) && noScreen) {
            client.gui.setScreen(new TflDiagnosticsScreen(client.gui.screen()));
        }
    }

    private void showStartupNotice(Minecraft client) {
        if (noticeShown || !inWorld) return;
        noticeShown = true;
        if (settings.isCorrupt()) {
            notifyPlayer(client, Component.translatable("tflcore.notice.corrupt"));
        } else if (settings.wasMigrated()) {
            notifyPlayer(client, Component.translatable("tflcore.notice.migrated"));
        }
        if (settings.isReadOnly()) {
            notifyPlayer(client, Component.translatable("tflcore.notice.read_only"));
        }
        if (!settings.get().ui.tipsShown) {
            notifyPlayer(client, Component.translatable("tflcore.notice.tips"));
            settings.get().ui.tipsShown = true;
            settings.requestSave();
        }
    }

    private void afterScreenInit(Minecraft client, net.minecraft.client.gui.screens.Screen screen,
                                 int scaledWidth, int scaledHeight) {
        if (screen instanceof TitleScreen && settings.get().ui.customMainMenu) {
            client.execute(() -> {
                if (client.gui.screen() == screen) {
                    client.gui.setScreen(new TflMainMenuScreen());
                }
            });
            return;
        }
        if (screen instanceof PauseScreen) {
            Button tfl = Button.builder(Component.translatable("tflcore.button.settings"),
                            button -> client.gui.setScreen(new TflSettingsScreen(screen)))
                    .bounds(6, Math.max(4, screen.height - 28), 96, 20)
                    .build();
            Screens.getWidgets(screen).add(tfl);
        }
    }
}
