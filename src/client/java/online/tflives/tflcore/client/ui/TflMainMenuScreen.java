package online.tflives.tflcore.client.ui;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.client.gui.screens.multiplayer.JoinMultiplayerScreen;
import net.minecraft.client.gui.screens.options.OptionsScreen;
import net.minecraft.client.gui.screens.worldselection.SelectWorldScreen;
import net.minecraft.network.chat.Component;
import online.tflives.tflcore.client.BuildInfo;
import online.tflives.tflcore.client.TflCoreClient;
import online.tflives.tflcore.client.config.TflSettings;

/** First TFL-native main menu. Static fallback avoids reliance on GPU effects. */
public final class TflMainMenuScreen extends Screen {
    public TflMainMenuScreen() {
        super(Component.literal("TFL Selection"));
    }

    @Override
    protected void init() {
        int buttonWidth = Math.min(216, Math.max(120, this.width - 36));
        int x = (this.width - buttonWidth) / 2;
        int firstY = Math.max(62, (this.height - 5 * 24) / 2 + 12);
        addButton(x, firstY, buttonWidth, "Un jugador", () ->
                this.minecraft.gui.setScreen(new SelectWorldScreen(this)));
        addButton(x, firstY + 24, buttonWidth, "Multijugador", () ->
                this.minecraft.gui.setScreen(new JoinMultiplayerScreen(this)));
        addButton(x, firstY + 48, buttonWidth, "TFL Core  /  Configuracion", () ->
                this.minecraft.gui.setScreen(new TflSettingsScreen(this)));
        addButton(x, firstY + 72, buttonWidth, "Opciones de Minecraft", () ->
                this.minecraft.gui.setScreen(new OptionsScreen(this, this.minecraft.options)));
        addButton(x, firstY + 96, buttonWidth, "Salir de Minecraft", this.minecraft::stop);
    }

    private void addButton(int x, int y, int width, String label, Runnable action) {
        this.addRenderableWidget(Button.builder(Component.literal(label), button -> action.run())
                .bounds(x, y, width, 20).build());
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta) {
        TflSettings settings = TflCoreClient.settings().get();
        TflTheme theme = TflTheme.from(settings);
        graphics.fillGradient(0, 0, this.width, this.height, theme.top(), theme.bottom());
        graphics.fill(0, 0, Math.max(4, this.width / 80), this.height, theme.accent());

        // Lightweight animated light bars, no shaders/FBOS/timers or scene loading.
        if (settings.animatedBackground && !settings.reducedMotion) {
            long frameTime = System.currentTimeMillis();
            int x = (int) ((frameTime / 38L) % Math.max(1, this.width + 220)) - 110;
            graphics.fill(x, 0, x + 2, this.height, 0x1535A9FF);
            graphics.fill(x + 20, 0, x + 22, this.height, 0x0D8ED7FF);
        }
        super.extractRenderState(graphics, mouseX, mouseY, delta);
        graphics.centeredText(this.font, "TFL SELECTION", this.width / 2,
                Math.max(19, this.height / 2 - 103), theme.text());
        graphics.centeredText(this.font, "UNA EXPERIENCIA. TODOS LOS MODOS.", this.width / 2,
                Math.max(33, this.height / 2 - 86), theme.muted());
        graphics.text(this.font, "TFL CORE  " + BuildInfo.VERSION, 12, this.height - 18,
                theme.muted(), false);
        graphics.text(this.font, "Minecraft 26.3  |  Fabric", 12, this.height - 30,
                theme.muted(), false);
    }

    @Override
    public boolean shouldCloseOnEsc() {
        return false;
    }
}
