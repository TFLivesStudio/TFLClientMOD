package online.tflives.tflcore.client.ui;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import online.tflives.tflcore.client.BuildInfo;
import online.tflives.tflcore.client.TflCoreClient;
import online.tflives.tflcore.client.compat.ModDetector;
import online.tflives.tflcore.client.config.SettingsStore;
import online.tflives.tflcore.client.hud.Benchmark;
import online.tflives.tflcore.client.hud.Diagnostics;
import online.tflives.tflcore.client.module.ModuleManager;
import online.tflives.tflcore.client.ui.widget.TflButton;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/** Read-only diagnostics: real frame percentiles, memory, modules, detected mods and storage state. */
public final class TflDiagnosticsScreen extends Screen {
    private final Screen parent;
    private final List<String> lines = new ArrayList<>(24);
    private int panelX;
    private int panelY;
    private int panelWidth;
    private int panelHeight;
    private int textX;
    private int textY;
    private int backupCount;
    private int hudWidgetCount;
    private String settingsPath = "";
    private String compatLine = "";

    public TflDiagnosticsScreen(Screen parent) {
        super(Component.translatable("tflcore.diagnostics.title"));
        this.parent = parent;
    }

    @Override
    public boolean isPauseScreen() {
        return true;
    }

    @Override
    protected void init() {
        panelWidth = Math.min(620, this.width - 20);
        panelHeight = Math.min(330, this.height - 36);
        panelX = (this.width - panelWidth) / 2;
        panelY = (this.height - panelHeight) / 2;
        textX = panelX + 14;
        textY = panelY + 30;
        backupCount = TflCoreClient.settings().backups().size();
        hudWidgetCount = TflCoreClient.hud().widgets().size();
        settingsPath = TflCoreClient.settings().path().toString();
        StringBuilder compat = new StringBuilder();
        for (Map.Entry<String, Boolean> entry : ModDetector.snapshot().entrySet()) {
            if (!compat.isEmpty()) compat.append("   ");
            compat.append(entry.getKey()).append(entry.getValue() ? " yes" : " no");
        }
        compatLine = compat.toString();

        int buttonY = panelY + panelHeight - 26;
        this.addRenderableWidget(new TflButton(panelX + 12, buttonY, 110, 18,
                Component.translatable("tflcore.diagnostics.benchmark"),
                TflButton.Style.SECONDARY, TflCoreClient::theme, () -> {
            Benchmark.start(this.minecraft, TflCoreClient.settings().get().perf.benchmarkSeconds);
            this.minecraft.gui.setScreen(parent);
        }));
        this.addRenderableWidget(new TflButton(panelX + 128, buttonY, 110, 18,
                Component.translatable("tflcore.diagnostics.reset_samples"),
                TflButton.Style.SECONDARY, TflCoreClient::theme, Diagnostics::reset));
        this.addRenderableWidget(new TflButton(panelX + 244, buttonY, 100, 18,
                Component.translatable("tflcore.diagnostics.settings"),
                TflButton.Style.SECONDARY, TflCoreClient::theme,
                () -> this.minecraft.gui.setScreen(new TflSettingsScreen(this))));
        this.addRenderableWidget(new TflButton(panelX + panelWidth - 100, buttonY, 88, 18,
                Component.translatable("gui.done"), TflButton.Style.PRIMARY, TflCoreClient::theme,
                this::onClose));
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta) {
        Diagnostics.sampleFrame(this.minecraft);
        TflTheme theme = TflCoreClient.theme();
        graphics.fillGradient(0, 0, this.width, this.height, theme.bgTop(), theme.bgBottom());
        graphics.fill(panelX, panelY, panelX + panelWidth, panelY + panelHeight, theme.surface());
        graphics.outline(panelX, panelY, panelWidth, panelHeight, theme.border());
        graphics.fill(panelX, panelY, panelX + 3, panelY + panelHeight, theme.accent());
        graphics.text(this.font, Component.translatable("tflcore.diagnostics.title").getString()
                        + "  //  " + BuildInfo.VERSION,
                textX, panelY + 9, theme.text(), false);

        buildLines();
        int y = textY;
        int maxWidth = panelWidth - 26;
        for (String line : lines) {
            boolean accent = line.startsWith(">");
            String text = accent ? line.substring(1) : line;
            graphics.text(this.font, TflDraw.trim(this.font, text, maxWidth), textX, y,
                    accent ? theme.accent() : theme.muted(), false);
            y += 11;
        }
        super.extractRenderState(graphics, mouseX, mouseY, delta);
    }

    private void buildLines() {
        lines.clear();
        SettingsStore store = TflCoreClient.settings();
        var settings = store.get();
        ModuleManager modules = TflCoreClient.modules();

        lines.add("> " + Component.translatable("tflcore.diagnostics.frames").getString());
        lines.add(String.format(Locale.ROOT,
                "FPS %d   samples %d   age %s",
                this.minecraft.getFps(), Diagnostics.samples(),
                Diagnostics.lastSampleAgeMillis() < 0 ? "n/a" : Diagnostics.lastSampleAgeMillis() + " ms"));
        lines.add(String.format(Locale.ROOT,
                "avg %.2f ms   p50 %.2f   p95 %.2f   p99 %.2f",
                Diagnostics.averageMillis(), Diagnostics.percentileMillis(50),
                Diagnostics.percentileMillis(95), Diagnostics.percentileMillis(99)));
        lines.add(String.format(Locale.ROOT, "min %.2f ms   max %.2f ms",
                Diagnostics.minMillis(), Diagnostics.maxMillis()));
        lines.add(Component.translatable("tflcore.diagnostics.bench_last", Benchmark.lastResult()).getString());
        if (Benchmark.running()) {
            lines.add(Component.translatable("tflcore.diagnostics.bench_running",
                    Benchmark.remainingMillis() / 1000).getString());
        }

        lines.add("> " + Component.translatable("tflcore.diagnostics.runtime").getString());
        lines.add(String.format(Locale.ROOT, "MC %s   Fabric Loader %s   Java %s",
                ModDetector.minecraftVersion(), ModDetector.fabricLoaderVersion(),
                System.getProperty("java.version", "?")));
        lines.add(String.format(Locale.ROOT, "RAM %d / %d MB   profile %s   theme %s",
                Diagnostics.usedMemoryMb(), Diagnostics.maxMemoryMb(),
                TflCoreClient.profile().name(), settings.ui.theme));
        lines.add(String.format(Locale.ROOT, "Modules %d/%d   HUD %d/%d   Waypoints %d",
                modules.enabledCount(), modules.registeredCount(),
                TflCoreClient.hud().enabledCount(), hudWidgetCount,
                settings.survival.waypoints.size()));

        lines.add("> " + Component.translatable("tflcore.diagnostics.compat").getString());
        lines.add(compatLine);

        lines.add("> " + Component.translatable("tflcore.diagnostics.storage").getString());
        lines.add(String.format(Locale.ROOT, "schema %d   %s   %s   %s   backups %d",
                settings.schemaVersion,
                store.isReadOnly() ? "READ-ONLY" : "writable",
                store.isCorrupt() ? "corrupt" : "ok",
                store.wasMigrated() ? "migrated" : "native",
                backupCount));
        lines.add(settingsPath);

        List<ModuleManager.ModuleError> errors = modules.errors();
        if (!errors.isEmpty()) {
            lines.add("> " + Component.translatable("tflcore.diagnostics.errors").getString());
            for (int i = 0; i < Math.min(3, errors.size()); i++) {
                ModuleManager.ModuleError error = errors.get(i);
                lines.add(error.module() + ": " + error.shortMessage());
            }
        }
    }

    @Override
    public void onClose() {
        this.minecraft.gui.setScreen(parent);
    }
}
