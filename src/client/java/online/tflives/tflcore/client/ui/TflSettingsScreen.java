package online.tflives.tflcore.client.ui;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import online.tflives.tflcore.client.TflCoreClient;
import online.tflives.tflcore.client.config.TflSettings;

import java.util.function.Consumer;

/** Small paged settings UI; every toggle persists immediately. */
public final class TflSettingsScreen extends Screen {
    private enum Page { GENERAL, HUD, PROFILE }

    private final Screen parent;
    private Page page = Page.GENERAL;

    public TflSettingsScreen(Screen parent) {
        super(Component.literal("TFL Core - Configuracion"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        int panelWidth = Math.min(330, Math.max(168, this.width - 28));
        int x = (this.width - panelWidth) / 2;
        int tabWidth = (panelWidth - 8) / 3;
        int tabY = Math.max(27, this.height / 2 - 91);
        tab(x, tabY, tabWidth, "General", Page.GENERAL);
        tab(x + tabWidth + 4, tabY, tabWidth, "HUD", Page.HUD);
        tab(x + (tabWidth + 4) * 2, tabY, tabWidth, "Perfiles", Page.PROFILE);

        TflSettings settings = TflCoreClient.settings().get();
        int y = tabY + 27;
        switch (page) {
            case GENERAL -> {
                toggle(x, y, panelWidth, "Menu propio", settings.customMainMenu,
                        s -> s.customMainMenu = !s.customMainMenu);
                toggle(x, y + 24, panelWidth, "Fondo animado", settings.animatedBackground,
                        s -> s.animatedBackground = !s.animatedBackground);
                toggle(x, y + 48, panelWidth, "Reducir movimiento", settings.reducedMotion,
                        s -> s.reducedMotion = !s.reducedMotion);
                control(x, y + 72, panelWidth, "Tema: " + settings.theme, () -> {
                    settings.theme = switch (settings.theme) {
                        case "BLUE" -> "MIDNIGHT";
                        case "MIDNIGHT" -> "FROST";
                        default -> "BLUE";
                    };
                    commit();
                });
            }
            case HUD -> {
                toggle(x, y, panelWidth, "Mostrar HUD TFL", settings.hudEnabled,
                        s -> s.hudEnabled = !s.hudEnabled);
                toggle(x, y + 24, panelWidth, "Contador FPS", settings.showFps,
                        s -> s.showFps = !s.showFps);
                toggle(x, y + 48, panelWidth, "Coordenadas", settings.showCoordinates,
                        s -> s.showCoordinates = !s.showCoordinates);
                toggle(x, y + 72, panelWidth, "Reloj del equipo", settings.showClock,
                        s -> s.showClock = !s.showClock);
            }
            case PROFILE -> {
                control(x, y, panelWidth, "Perfil: " + settings.preset, () -> {
                    String next = switch (settings.preset) {
                        case "BALANCED" -> "FPS";
                        case "FPS" -> "QUALITY";
                        default -> "BALANCED";
                    };
                    settings.applyPreset(next);
                    commit();
                });
                control(x, y + 24, panelWidth, "Mover HUD horizontal  +12", () -> {
                    settings.hudX = Math.min(settings.hudX + 12, 10000);
                    commit();
                });
                control(x, y + 48, panelWidth, "Mover HUD vertical  +12", () -> {
                    settings.hudY = Math.min(settings.hudY + 12, 10000);
                    commit();
                });
                control(x, y + 72, panelWidth, "Reiniciar posicion HUD", () -> {
                    settings.hudX = 8;
                    settings.hudY = 8;
                    commit();
                });
            }
        }
        control(x, y + 102, panelWidth, "Volver", this::onClose);
    }

    private void tab(int x, int y, int width, String name, Page target) {
        this.addRenderableWidget(Button.builder(Component.literal(
                page == target ? "[" + name + "]" : name), b -> {
            page = target;
            rebuildWidgets();
        }).bounds(x, y, width, 20).build());
    }

    private void toggle(int x, int y, int width, String name, boolean enabled, Consumer<TflSettings> action) {
        control(x, y, width, name + "  [" + (enabled ? "ON" : "OFF") + "]", () -> {
            action.accept(TflCoreClient.settings().get());
            TflCoreClient.settings().get().preset = "CUSTOM";
            commit();
        });
    }

    private void control(int x, int y, int width, String name, Runnable action) {
        this.addRenderableWidget(Button.builder(Component.literal(name), b -> action.run())
                .bounds(x, y, width, 20).build());
    }

    private void commit() {
        TflCoreClient.settings().save();
        rebuildWidgets();
    }

    @Override
    public void onClose() {
        this.minecraft.gui.setScreen(parent);
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta) {
        TflTheme theme = TflTheme.from(TflCoreClient.settings().get());
        graphics.fillGradient(0, 0, this.width, this.height, theme.top(), theme.bottom());
        graphics.fill(0, 0, 4, this.height, theme.accent());
        super.extractRenderState(graphics, mouseX, mouseY, delta);
        graphics.centeredText(this.font, "TFL CORE  /  CONFIGURACION", this.width / 2,
                Math.max(8, this.height / 2 - 110), theme.text());
        graphics.centeredText(this.font, "Los cambios se guardan automaticamente", this.width / 2,
                this.height - 16, theme.muted());
    }
}
