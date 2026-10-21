package online.tflives.tflcore.client.ui;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.network.chat.Component;
import online.tflives.tflcore.client.BuildInfo;
import online.tflives.tflcore.client.TflCoreClient;
import online.tflives.tflcore.client.config.SettingsStore;
import online.tflives.tflcore.client.config.TflSettings;
import online.tflives.tflcore.client.config.WidgetPlacement;
import online.tflives.tflcore.client.hud.HudWidget;
import online.tflives.tflcore.client.keybind.KeybindManager;
import online.tflives.tflcore.client.module.ModuleManager;
import online.tflives.tflcore.client.profile.ClientProfile;
import online.tflives.tflcore.client.ui.widget.TflButton;
import online.tflives.tflcore.client.ui.widget.TflSlider;
import online.tflives.tflcore.client.ui.widget.TflToggle;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;

/**
 * Tabbed TFL Core settings.
 *
 * <p>Every value is validated by {@link TflSettings#normalize()} and persisted through the debounced
 * {@link SettingsStore}. Dangerous actions require a second click within four seconds, and file
 * import/export is restricted to the Minecraft config directory.</p>
 */
public final class TflSettingsScreen extends Screen {
    private enum Tab {
        GENERAL("tflcore.settings.tab.general"),
        APPEARANCE("tflcore.settings.tab.appearance"),
        HUD("tflcore.settings.tab.hud"),
        GAME("tflcore.settings.tab.game"),
        KEYS("tflcore.settings.tab.keys"),
        ADVANCED("tflcore.settings.tab.advanced");

        private final String key;

        Tab(String key) {
            this.key = key;
        }
    }

    private static final String[] THEMES = {"BLUE", "MIDNIGHT", "FROST", "EMBER", "MONO"};
    private static final String[] ACCENTS = {"DEFAULT", "CYAN", "VIOLET", "AMBER", "LIME", "ROSE"};
    private static final String[] STYLES = {"CROSS", "DOT", "T", "CIRCLE", "SQUARE"};
    private static final int[] CROSSHAIR_COLORS = {
            0xFFFFFFFF, 0xFF35A9FF, 0xFF7CE07C, 0xFFFF6E6E, 0xFFFFD166, 0xFFC77DFF
    };

    private final Screen parent;
    private EditBox searchBox;
    private Tab tab = Tab.GENERAL;
    private int hudPage;
    private String hudFilter = "";
    private String status = "";
    private float statusUntil;
    private boolean confirmReset;
    private float confirmUntil;

    private int panelX;
    private int panelY;
    private int panelWidth;
    private int rowX;
    private int rowWidth;
    private int contentTop;
    private int contentBottom;
    private int keysInfoY;
    private int advancedInfoY;
    private int advancedHintY;
    private String advancedPath = "";
    private int advancedBackups;
    private int hudPageLabelY;
    private int hudPages = 1;
    private int hudTotal;

    public TflSettingsScreen(Screen parent) {
        super(Component.translatable("tflcore.settings.title"));
        this.parent = parent;
    }

    @Override
    public boolean isPauseScreen() {
        return true;
    }

    @Override
    protected void init() {
        TflSettings settings = TflCoreClient.settings().get();
        // Snapshot storage state once per rebuild instead of querying the filesystem every frame.
        advancedPath = TflCoreClient.settings().path().toString();
        advancedBackups = TflCoreClient.settings().backups().size();
        panelWidth = Math.min(680, this.width - 20);
        panelX = (this.width - panelWidth) / 2;
        panelY = 8;
        rowX = panelX + 14;
        rowWidth = panelWidth - 28;

        int rows = panelWidth >= 560 ? 1 : 2;
        int perRow = rows == 1 ? 6 : 3;
        int gap = 4;
        int tabWidth = (rowWidth - gap * (perRow - 1)) / perRow;
        int tabsY = panelY + 20;
        Tab[] tabs = Tab.values();
        for (int i = 0; i < tabs.length; i++) {
            Tab value = tabs[i];
            int row = i / perRow;
            int column = i % perRow;
            int x = rowX + column * (tabWidth + gap);
            int y = tabsY + row * 24;
            this.addRenderableWidget(new TflButton(x, y, tabWidth, 20,
                    Component.translatable(value.key), TflButton.Style.TAB, TflCoreClient::theme, () -> {
                tab = value;
                hudPage = 0;
                rebuildWidgets();
            }).selected(tab == value));
        }
        contentTop = tabsY + rows * 24 + 4;
        contentBottom = this.height - 32;

        int y = contentTop + 2;
        switch (tab) {
            case GENERAL -> buildGeneral(settings, y);
            case APPEARANCE -> buildAppearance(settings, y);
            case HUD -> buildHud(settings, y);
            case GAME -> buildGame(settings, y);
            case KEYS -> buildKeys(y);
            case ADVANCED -> buildAdvanced(settings, y);
        }

        this.addRenderableWidget(new TflButton(this.width - 110, this.height - 26, 100, 20,
                Component.translatable("gui.done"), TflButton.Style.PRIMARY, TflCoreClient::theme,
                this::onClose));
    }

    private void buildGeneral(TflSettings settings, int y) {
        toggle(y, "tflcore.settings.hud_enabled", () -> settings.hud.enabled,
                value -> settings.hud.enabled = value);
        y += rowStep();
        toggle(y, "tflcore.settings.custom_menu", () -> settings.ui.customMainMenu,
                value -> settings.ui.customMainMenu = value);
        y += rowStep();
        toggle(y, "tflcore.settings.animated_background", () -> settings.ui.animatedBackground,
                value -> settings.ui.animatedBackground = value);
        y += rowStep();
        toggle(y, "tflcore.settings.reduced_motion", () -> settings.ui.reducedMotion,
                value -> settings.ui.reducedMotion = value);
        y += rowStep();
        toggle(y, "tflcore.settings.streamer_mode", () -> settings.hud.streamerMode,
                value -> settings.hud.streamerMode = value);
        y += rowStep();
        action(y, Component.translatable("tflcore.settings.profile",
                        Component.translatable(TflCoreClient.profile().translationKey())),
                () -> {
                    ClientProfile next = TflCoreClient.profile().next();
                    TflCoreClient.applyProfile(next);
                    note(Component.translatable("tflcore.menu.profile_applied",
                            Component.translatable(next.translationKey())));
                    rebuildWidgets();
                });
        y += rowStep();
        action(y, Component.translatable("tflcore.settings.preset", settings.perf.preset), () -> {
            String next = switch (settings.perf.preset) {
                case "BALANCED" -> "FPS";
                case "FPS" -> "QUALITY";
                default -> "BALANCED";
            };
            settings.applyPreset(next);
            save();
            note(Component.translatable("tflcore.settings.preset_applied", next));
            rebuildWidgets();
        });
        y += rowStep();
        action(y, Component.translatable("tflcore.settings.open_editor"), () -> {
            if (this.minecraft.player == null) {
                note(Component.translatable("tflcore.notice.world_required"));
                return;
            }
            this.minecraft.gui.setScreen(new TflHudEditorScreen(this));
        });
        y += rowStep();
        action(y, Component.translatable("tflcore.settings.open_diagnostics"),
                () -> this.minecraft.gui.setScreen(new TflDiagnosticsScreen(this)));
    }

    private void buildAppearance(TflSettings settings, int y) {
        action(y, Component.translatable("tflcore.settings.theme", settings.ui.theme), () -> {
            settings.ui.theme = cycle(THEMES, settings.ui.theme);
            save();
            rebuildWidgets();
        });
        y += rowStep();
        action(y, Component.translatable("tflcore.settings.accent", settings.ui.accent), () -> {
            settings.ui.accent = cycle(ACCENTS, settings.ui.accent);
            save();
            rebuildWidgets();
        });
        y += rowStep() + 2;
        this.addRenderableWidget(new TflSlider(rowX, y, rowWidth, 24,
                Component.translatable("tflcore.settings.transition"), 0, 400, 20, " ms",
                () -> settings.ui.transitionMillis,
                value -> {
                    settings.ui.transitionMillis = value;
                    save();
                }, TflCoreClient::theme));
        y += 28;
        toggle(y, "tflcore.settings.high_contrast", () -> settings.ui.highContrast,
                value -> settings.ui.highContrast = value);
        y += rowStep();
        toggle(y, "tflcore.settings.reduced_transparency", () -> settings.ui.reducedTransparency,
                value -> settings.ui.reducedTransparency = value);
        y += rowStep();
        toggle(y, "tflcore.settings.animated_background", () -> settings.ui.animatedBackground,
                value -> settings.ui.animatedBackground = value);
        y += rowStep();
        toggle(y, "tflcore.settings.reduced_motion", () -> settings.ui.reducedMotion,
                value -> settings.ui.reducedMotion = value);
        y += rowStep();
        action(y, Component.translatable("tflcore.settings.reset_appearance"), () -> {
            settings.ui.theme = "BLUE";
            settings.ui.accent = "DEFAULT";
            settings.ui.highContrast = false;
            settings.ui.reducedTransparency = false;
            settings.ui.animatedBackground = true;
            settings.ui.reducedMotion = false;
            save();
            note(Component.translatable("tflcore.settings.reset_done"));
            rebuildWidgets();
        });
    }

    private void buildHud(TflSettings settings, int y) {
        toggle(y, "tflcore.settings.hud_enabled", () -> settings.hud.enabled,
                value -> settings.hud.enabled = value);
        y += rowStep();

        searchBox = new EditBox(this.font, rowX, y, rowWidth - 96, 18,
                Component.translatable("tflcore.settings.search"));
        searchBox.setMaxLength(32);
        searchBox.setValue(hudFilter);
        searchBox.setResponder(value -> hudFilter = value);
        this.addRenderableWidget(searchBox);
        actionAt(rowX + rowWidth - 90, y - 1, 90, 20,
                Component.translatable("tflcore.settings.apply_filter"), () -> {
                    hudPage = 0;
                    rebuildWidgets();
                });
        y += rowStep() + 2;

        List<HudWidget> widgets = filteredWidgets();
        int pageSize = Math.max(3, (contentBottom - y - 82) / rowStep());
        hudTotal = widgets.size();
        int pages = Math.max(1, (widgets.size() + pageSize - 1) / pageSize);
        hudPages = pages;
        if (hudPage >= pages) hudPage = pages - 1;
        int start = hudPage * pageSize;
        int end = Math.min(widgets.size(), start + pageSize);
        for (int i = start; i < end; i++) {
            HudWidget widget = widgets.get(i);
            WidgetPlacement placement = settings.widget(widget.id(), widget.defaultPlacement());
            this.addRenderableWidget(new TflToggle(rowX, y, rowWidth, rowStep() - 2,
                    Component.translatable(widget.nameKey()).append(
                            Component.literal("  (" + widget.id() + ")")),
                    () -> placement.enabled,
                    value -> {
                        placement.enabled = value;
                        save();
                    }, TflCoreClient::theme));
            y += rowStep();
        }

        int navY = contentBottom - 24;
        actionAt(rowX, navY, 34, 20, Component.literal("<"), () -> {
            hudPage = Math.max(0, hudPage - 1);
            rebuildWidgets();
        });
        actionAt(rowX + 38, navY, 34, 20, Component.literal(">"), () -> {
            hudPage = Math.min(pages - 1, hudPage + 1);
            rebuildWidgets();
        });
        hudPageLabelY = navY + 5;
        int actionY = contentBottom - 48;
        int half = (rowWidth - 4) / 2;
        actionAt(rowX, actionY, half, 20, Component.translatable("tflcore.settings.reset_layout"), () -> {
            TflCoreClient.hud().resetLayout();
            note(Component.translatable("tflcore.settings.reset_done"));
            rebuildWidgets();
        });
        actionAt(rowX + half + 4, actionY, half, 20, Component.translatable("tflcore.settings.open_editor"), () -> {
            if (this.minecraft.player == null) {
                note(Component.translatable("tflcore.notice.world_required"));
                return;
            }
            this.minecraft.gui.setScreen(new TflHudEditorScreen(this));
        });
    }

    private void buildGame(TflSettings settings, int y) {
        toggle(y, "tflcore.settings.custom_crosshair",
                () -> TflCoreClient.modules().isEnabled("crosshair") && settings.pvp.customCrosshair,
                value -> {
                    settings.pvp.customCrosshair = value;
                    TflCoreClient.modules().setEnabled("crosshair", value);
                });
        y += rowStep();
        action(y, Component.translatable("tflcore.settings.crosshair_style", settings.pvp.crosshairStyle), () -> {
            settings.pvp.crosshairStyle = cycle(STYLES, settings.pvp.crosshairStyle);
            save();
            rebuildWidgets();
        });
        y += rowStep();
        this.addRenderableWidget(new TflSlider(rowX, y, rowWidth, 24,
                Component.translatable("tflcore.settings.crosshair_scale"), 50, 300, 10, "%",
                () -> Math.round(settings.pvp.crosshairScale * 100F),
                value -> {
                    settings.pvp.crosshairScale = value / 100F;
                    save();
                }, TflCoreClient::theme));
        y += 28;
        action(y, Component.translatable("tflcore.settings.crosshair_color",
                Component.translatable(colorName(settings.pvp.crosshairColor))), () -> {
            settings.pvp.crosshairColor = nextColor(settings.pvp.crosshairColor);
            save();
            rebuildWidgets();
        });
        y += rowStep();
        toggle(y, "tflcore.settings.toggle_sprint",
                () -> TflCoreClient.modules().isEnabled("toggle-sprint"), value -> {
            TflCoreClient.modules().setEnabled("toggle-sprint", value);
            save();
        });
        y += rowStep();
        toggle(y, "tflcore.settings.zoom",
                () -> TflCoreClient.modules().isEnabled("zoom"), value -> {
            TflCoreClient.modules().setEnabled("zoom", value);
            save();
        });
        y += rowStep();
        this.addRenderableWidget(new TflSlider(rowX, y, rowWidth, 24,
                Component.translatable("tflcore.settings.zoom_fov"), 10, 90, 1, "\u00B0",
                () -> settings.survival.zoomFov,
                value -> {
                    settings.survival.zoomFov = value;
                    save();
                }, TflCoreClient::theme));
        y += 28;
        toggle(y, "tflcore.settings.smooth_zoom", () -> settings.survival.smoothZoom,
                value -> settings.survival.smoothZoom = value);
        y += rowStep();
        toggle(y, "tflcore.settings.death_waypoints", () -> settings.survival.deathWaypoint,
                value -> settings.survival.deathWaypoint = value);
    }

    private void buildKeys(int y) {
        for (KeybindManager.Action action : KeybindManager.Action.values()) {
            KeybindManager keybinds = TflCoreClient.keybinds();
            boolean waiting = keybinds.rebindingAction() == action;
            Component label = Component.translatable(action.translationKey())
                    .append(Component.literal("   "))
                    .append(waiting ? Component.translatable("tflcore.settings.press_key")
                            : KeybindManager.describe(keybinds.mapping(action)));
            action(y, label, () -> {
                keybinds.beginRebind(action);
                note(Component.translatable("tflcore.settings.press_key"));
                rebuildWidgets();
            });
            y += rowStep();
        }
        this.keysInfoY = y + 2;
        action(y + 34, Component.translatable("tflcore.settings.reset_keys"), () -> {
            TflCoreClient.keybinds().resetDefaults();
            note(Component.translatable("tflcore.settings.reset_done"));
            rebuildWidgets();
        });
    }

    private void buildAdvanced(TflSettings settings, int y) {
        this.advancedInfoY = y;
        y += 58;

        List<ModuleManager.ModuleInfo> configurable = new ArrayList<>();
        for (ModuleManager.ModuleInfo info : TflCoreClient.modules().info()) {
            if (info.configurable()) {
                configurable.add(info);
            }
        }
        if (this.height < 300 && configurable.size() > 1) {
            int gap = 6;
            int columnWidth = (rowWidth - gap) / 2;
            for (int i = 0; i < configurable.size(); i++) {
                int x = rowX + (i % 2) * (columnWidth + gap);
                int rowY = y + (i / 2) * rowStep();
                addModuleToggle(configurable.get(i), x, rowY, columnWidth);
            }
            y += ((configurable.size() + 1) / 2) * rowStep();
        } else {
            for (ModuleManager.ModuleInfo info : configurable) {
                addModuleToggle(info, rowX, y, rowWidth);
                y += rowStep();
            }
        }
        y += 2;

        Runnable doExport = () -> {
            Path target = TflCoreClient.settings().configRoot().resolve("tflcore-export.json");
            SettingsStore.ImportResult result = TflCoreClient.settings().exportTo(target);
            note(Component.translatable("tflcore.settings.export_result", result.name(),
                    target.getFileName().toString()));
        };
        Runnable doImport = () -> {
            SettingsStore store = TflCoreClient.settings();
            SettingsStore.ImportResult result = store.importFrom(
                    store.configRoot().resolve("tflcore-import.json"));
            note(Component.translatable("tflcore.settings.import_result", result.name()));
            if (result == SettingsStore.ImportResult.OK) {
                rebuildWidgets();
            }
        };
        Runnable doReset = () -> {
            if (confirmReset && UiClock.seconds() < confirmUntil) {
                TflCoreClient.settings().resetToDefaults();
                confirmReset = false;
                note(Component.translatable("tflcore.settings.reset_done"));
                rebuildWidgets();
            } else {
                confirmReset = true;
                confirmUntil = UiClock.seconds() + 4F;
                note(Component.translatable("tflcore.settings.reset_confirm"));
            }
        };

        if (this.height < 340) {
            int gap = 4;
            int actionWidth = (rowWidth - gap * 2) / 3;
            actionAt(rowX, y, actionWidth, rowStep() - 2,
                    Component.translatable("tflcore.settings.export"), doExport);
            actionAt(rowX + actionWidth + gap, y, actionWidth, rowStep() - 2,
                    Component.translatable("tflcore.settings.import"), doImport);
            boolean confirming = confirmReset && UiClock.seconds() < confirmUntil;
            actionAt(rowX + 2 * (actionWidth + gap), y, actionWidth, rowStep() - 2,
                    Component.translatable(confirming
                            ? "tflcore.settings.reset_confirm" : "tflcore.settings.reset_all"), doReset);
            y += rowStep();
        } else {
            action(y, Component.translatable("tflcore.settings.export"), doExport);
            y += rowStep();
            action(y, Component.translatable("tflcore.settings.import"), doImport);
            y += rowStep();
            boolean confirming = confirmReset && UiClock.seconds() < confirmUntil;
            actionAt(rowX, y, rowWidth, rowStep() - 2,
                    Component.translatable(confirming
                            ? "tflcore.settings.reset_confirm" : "tflcore.settings.reset_all"), doReset);
            y += rowStep();
        }
        this.advancedHintY = y + 2;
    }

    private void addModuleToggle(ModuleManager.ModuleInfo info, int x, int y, int width) {
        this.addRenderableWidget(new TflToggle(x, y, width, rowStep() - 2,
                Component.translatable("tflcore.module." + info.id() + ".name"),
                () -> TflCoreClient.modules().isEnabled(info.id()),
                value -> {
                    TflCoreClient.modules().setEnabled(info.id(), value);
                    save();
                }, TflCoreClient::theme));
    }

    private List<HudWidget> filteredWidgets() {
        List<HudWidget> result = new ArrayList<>();
        String filter = hudFilter == null ? "" : hudFilter.strip().toLowerCase(Locale.ROOT);
        for (HudWidget widget : TflCoreClient.hud().widgets()) {
            if (filter.isEmpty()
                    || widget.id().contains(filter)
                    || Component.translatable(widget.nameKey()).getString().toLowerCase(Locale.ROOT).contains(filter)) {
                result.add(widget);
            }
        }
        return result;
    }

    private int rowStep() {
        if (this.height < 300) return 20;
        if (this.height < 340) return 21;
        return 24;
    }

    private void toggle(int y, String labelKey, BooleanSupplier getter, Consumer<Boolean> setter) {
        this.addRenderableWidget(new TflToggle(rowX, y, rowWidth, rowStep() - 2,
                Component.translatable(labelKey), getter, value -> {
                    setter.accept(value);
                    save();
                }, TflCoreClient::theme));
    }

    private void action(int y, Component label, Runnable action) {
        actionAt(rowX, y, rowWidth, rowStep() - 2, label, action);
    }

    private void actionAt(int x, int y, int width, int height, Component label, Runnable action) {
        this.addRenderableWidget(new TflButton(x, y, width, height, label,
                TflButton.Style.SECONDARY, TflCoreClient::theme, action));
    }

    private void save() {
        TflSettings settings = TflCoreClient.settings().get();
        settings.markCustom();
        TflCoreClient.settings().requestSave();
    }

    private void note(Component message) {
        status = message.getString();
        statusUntil = UiClock.seconds() + 4F;
    }

    private static String cycle(String[] values, String current) {
        for (int i = 0; i < values.length; i++) {
            if (values[i].equalsIgnoreCase(current)) {
                return values[(i + 1) % values.length];
            }
        }
        return values[0];
    }

    private static int nextColor(int current) {
        for (int i = 0; i < CROSSHAIR_COLORS.length; i++) {
            if (CROSSHAIR_COLORS[i] == current) {
                return CROSSHAIR_COLORS[(i + 1) % CROSSHAIR_COLORS.length];
            }
        }
        return CROSSHAIR_COLORS[0];
    }

    private static String colorName(int color) {
        return switch (color) {
            case 0xFF35A9FF -> "tflcore.color.blue";
            case 0xFF7CE07C -> "tflcore.color.green";
            case 0xFFFF6E6E -> "tflcore.color.red";
            case 0xFFFFD166 -> "tflcore.color.amber";
            case 0xFFC77DFF -> "tflcore.color.violet";
            default -> "tflcore.color.white";
        };
    }

    @Override
    public boolean keyPressed(KeyEvent event) {
        if (TflCoreClient.keybinds().isRebinding()) {
            boolean cancelled = event.key() == InputConstants.KEY_ESCAPE;
            TflCoreClient.keybinds().handleRebindKey(this.minecraft, event);
            if (!cancelled) {
                note(Component.translatable("tflcore.settings.key_updated"));
            }
            rebuildWidgets();
            return true;
        }
        return super.keyPressed(event);
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta) {
        TflTheme theme = TflCoreClient.theme();
        graphics.fillGradient(0, 0, this.width, this.height, theme.bgTop(), theme.bgBottom());
        int bottom = this.height - 8;
        graphics.fill(panelX, panelY, panelX + panelWidth, bottom, theme.surface());
        graphics.outline(panelX, panelY, panelWidth, bottom - panelY, theme.border());
        graphics.fill(panelX, panelY, panelX + 3, bottom, theme.accent());

        int titleY = panelY + 6;
        graphics.text(this.font, Component.translatable("tflcore.settings.title").getString().toUpperCase(Locale.ROOT),
                rowX, titleY, theme.text(), false);
        TflDraw.rightAligned(graphics, this.font, "TFL CORE " + BuildInfo.VERSION,
                panelX + panelWidth - 14, titleY, theme.muted());

        super.extractRenderState(graphics, mouseX, mouseY, delta);

        switch (tab) {
            case HUD -> {
                graphics.text(this.font, (hudPage + 1) + " / " + hudPages + "   " + hudTotal + " "
                                + Component.translatable("tflcore.settings.widgets").getString(),
                        rowX + 78, hudPageLabelY, theme.muted(), false);
            }
            case KEYS -> {
                List<KeybindManager.Conflict> conflicts = TflCoreClient.keybinds().conflicts(this.minecraft);
                if (conflicts.isEmpty()) {
                    graphics.text(this.font, Component.translatable("tflcore.settings.no_conflicts").getString(),
                            rowX, keysInfoY, theme.success(), false);
                } else {
                    for (int i = 0; i < Math.min(3, conflicts.size()); i++) {
                        KeybindManager.Conflict conflict = conflicts.get(i);
                        graphics.text(this.font, Component.translatable("tflcore.settings.conflict",
                                        Component.translatable(conflict.action.translationKey()), conflict.otherBinding)
                                .getString(), rowX, keysInfoY + i * 11, theme.danger(), false);
                    }
                }
            }
            case ADVANCED -> drawAdvancedInfo(graphics, theme);
            default -> {
            }
        }

        if (!status.isEmpty()) {
            if (UiClock.seconds() < statusUntil) {
                graphics.text(this.font, TflDraw.trim(this.font, status, this.width - 150),
                        rowX, this.height - 20, theme.accent(), false);
            } else {
                status = "";
            }
        }
    }

    private void drawAdvancedInfo(GuiGraphicsExtractor graphics, TflTheme theme) {
        SettingsStore store = TflCoreClient.settings();
        TflSettings settings = store.get();
        String[] lines = {
                Component.translatable("tflcore.settings.schema", settings.schemaVersion).getString(),
                Component.translatable("tflcore.settings.path", advancedPath).getString(),
                Component.translatable("tflcore.settings.backups", advancedBackups).getString(),
                Component.translatable("tflcore.settings.flags",
                        store.isReadOnly() ? "RO" : "RW",
                        store.isCorrupt() ? "corrupt" : "ok",
                        store.wasMigrated() ? "migrated" : "native").getString(),
                Component.translatable("tflcore.settings.import_hint").getString()
        };
        for (int i = 0; i < lines.length; i++) {
            int color = i == 3 && (store.isReadOnly() || store.isCorrupt()) ? theme.danger() : theme.muted();
            graphics.text(this.font, TflDraw.trim(this.font, lines[i], rowWidth), rowX,
                    advancedInfoY + i * 11, color, false);
        }
        if (this.height >= 300) {
            graphics.text(this.font, Component.translatable("tflcore.settings.import_help").getString(),
                    rowX, advancedHintY + 12, theme.muted(), false);
        }
    }

    @Override
    public void onClose() {
        TflCoreClient.settings().flush();
        if (parent == null) {
            this.minecraft.gui.setScreen(null);
            return;
        }
        if (!TflCoreClient.settings().get().ui.customMainMenu
                && (parent instanceof TflMainMenuScreen)) {
            this.minecraft.gui.setScreen(new TitleScreen());
            return;
        }
        this.minecraft.gui.setScreen(parent);
    }

    @Override
    public void removed() {
        TflCoreClient.settings().flush();
        super.removed();
    }
}
