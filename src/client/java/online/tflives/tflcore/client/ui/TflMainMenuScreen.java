package online.tflives.tflcore.client.ui;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.client.gui.screens.multiplayer.JoinMultiplayerScreen;
import net.minecraft.client.gui.screens.options.OptionsScreen;
import net.minecraft.client.gui.screens.worldselection.SelectWorldScreen;
import net.minecraft.network.chat.Component;
import online.tflives.tflcore.client.BuildInfo;
import online.tflives.tflcore.client.TflCoreClient;
import online.tflives.tflcore.client.config.TflSettings;
import online.tflives.tflcore.client.profile.ClientProfile;
import online.tflives.tflcore.client.ui.widget.TflButton;

import java.util.Locale;

/**
 * TFL Selection main menu.
 *
 * <p>Replaces the vanilla title screen when enabled. The design uses the shared token system, a
 * brand column, large mode cards and a status strip. Animations are tick-driven (never wall-clock)
 * and can be disabled entirely for accessibility or performance.</p>
 */
public final class TflMainMenuScreen extends Screen {
    private boolean compact;
    private int brandX;
    private int brandWidth;
    private int cardsX;
    private int cardsY;
    private int cardsWidth;
    private String status = "";
    private float statusUntil;

    public TflMainMenuScreen() {
        super(Component.translatable("tflcore.menu.title"));
    }

    @Override
    protected void init() {
        TflSettings settings = TflCoreClient.settings().get();
        compact = this.width < 440 || this.height < 310;
        int gap = compact ? 4 : 8;
        int cardHeight = compact ? 26 : 42;
        cardsWidth = compact ? Math.min(300, this.width - 24) : Math.min(360, this.width - 48);
        int totalHeight = cardHeight * 5 + gap * 4 + 12 + 24;
        cardsX = compact ? (this.width - cardsWidth) / 2
                : Math.max(this.width / 2 + 24, this.width - cardsWidth - 30);
        cardsY = compact ? Math.max(14, (this.height - totalHeight) / 2 - 8)
                : Math.max(64, (this.height - totalHeight) / 2);
        brandX = 28;
        brandWidth = cardsX - 56;
        if (brandWidth < 130) {
            compact = true;
            cardsWidth = Math.min(320, this.width - 24);
            cardsX = (this.width - cardsWidth) / 2;
            cardsY = Math.max(10, (this.height - totalHeight) / 2 - 6);
        }

        int y = cardsY;
        addCard(cardsX, y, cardsWidth, cardHeight, "tflcore.menu.singleplayer",
                "tflcore.menu.singleplayer.desc", TflButton.Style.SECONDARY,
                () -> this.minecraft.gui.setScreen(new SelectWorldScreen(this)));
        y += cardHeight + gap;
        addCard(cardsX, y, cardsWidth, cardHeight, "tflcore.menu.multiplayer",
                "tflcore.menu.multiplayer.desc", TflButton.Style.SECONDARY,
                () -> this.minecraft.gui.setScreen(new JoinMultiplayerScreen(this)));
        y += cardHeight + gap;
        addCard(cardsX, y, cardsWidth, cardHeight, "tflcore.menu.core",
                "tflcore.menu.core.desc", TflButton.Style.PRIMARY,
                () -> this.minecraft.gui.setScreen(new TflSettingsScreen(this)));
        y += cardHeight + gap;
        addCard(cardsX, y, cardsWidth, cardHeight, "tflcore.menu.options",
                "tflcore.menu.options.desc", TflButton.Style.SECONDARY,
                () -> this.minecraft.gui.setScreen(new OptionsScreen(this, this.minecraft.options)));
        y += cardHeight + gap;
        addCard(cardsX, y, cardsWidth, cardHeight, "tflcore.menu.quit",
                "tflcore.menu.quit.desc", TflButton.Style.DANGER, this.minecraft::stop);
        y += cardHeight + 12;

        ClientProfile profile = TflCoreClient.profile();
        this.addRenderableWidget(new TflButton(cardsX, y, cardsWidth, 24,
                Component.translatable("tflcore.menu.profile", profileName(profile)),
                TflButton.Style.GHOST, TflCoreClient::theme, () -> {
            ClientProfile next = TflCoreClient.profile().next();
            TflCoreClient.applyProfile(next);
            status = Component.translatable("tflcore.menu.profile_applied", profileName(next)).getString();
            statusUntil = UiClock.seconds() + 4F;
            rebuildWidgets();
        }).trailing(">"));
    }

    private void addCard(int x, int y, int width, int height, String labelKey, String subtitleKey,
                         TflButton.Style style, Runnable action) {
        Component subtitle = compact ? Component.empty() : Component.translatable(subtitleKey);
        this.addRenderableWidget(new TflButton(x, y, width, height,
                Component.translatable(labelKey), style, TflCoreClient::theme, action)
                .subtitle(subtitle));
    }

    private static String profileName(ClientProfile profile) {
        return Component.translatable(profile.translationKey()).getString();
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta) {
        TflSettings settings = TflCoreClient.settings().get();
        TflTheme theme = TflCoreClient.theme();
        graphics.fillGradient(0, 0, this.width, this.height, theme.bgTop(), theme.bgBottom());
        drawBackdrop(graphics, theme, settings, delta);
        graphics.fill(0, 0, this.width, 2, theme.accent());

        super.extractRenderState(graphics, mouseX, mouseY, delta);

        if (!compact) {
            drawBrand(graphics, theme, settings);
        } else {
            graphics.centeredText(this.font, "TFL SELECTION", this.width / 2, 6, theme.text());
        }
        drawFooter(graphics, theme, settings);
    }

    private void drawBackdrop(GuiGraphicsExtractor graphics, TflTheme theme, TflSettings settings, float delta) {
        float seconds = UiClock.seconds(delta);
        if (!settings.ui.reducedMotion && settings.ui.animatedBackground) {
            int span = this.width + 240;
            int shift = (int) ((seconds * 26F) % span) - 120;
            for (int i = 0; i < 3; i++) {
                int x = shift - i * 170;
                graphics.fill(x, 0, x + 1, this.height,
                        TflTheme.withAlpha(theme.accent(), 0.10F - i * 0.025F));
            }
            int scan = (int) ((seconds * 18F) % Math.max(1, this.height));
            graphics.fill(0, scan, this.width, scan + 1, TflTheme.withAlpha(theme.accent(), 0.05F));
        }

        // A subtle static grid gives the layout depth without any per-frame texture work.
        int step = 64;
        int grid = TflTheme.withAlpha(theme.text(), 0.035F);
        for (int x = 0; x < this.width; x += step) {
            graphics.fill(x, 0, x + 1, this.height, grid);
        }
        for (int y = 0; y < this.height; y += step) {
            graphics.fill(0, y, this.width, y + 1, grid);
        }
        graphics.fillGradient(0, this.height - 70, this.width, this.height, 0x00000000, 0x66000000);
    }

    private void drawBrand(GuiGraphicsExtractor graphics, TflTheme theme, TflSettings settings) {
        int top = Math.max(44, this.height / 2 - 108);
        graphics.fill(brandX, top, brandX + 30, top + 30, theme.accentSoft());
        graphics.outline(brandX, top, 30, 30, theme.accent());
        graphics.text(this.font, "TFL", brandX + 5, top + 11, theme.accent(), false);

        graphics.pose().pushMatrix();
        graphics.pose().translate(brandX, top + 42);
        graphics.pose().scale(2.15F, 2.15F);
        graphics.text(this.font, "SELECTION", 0, 0, theme.text(), false);
        graphics.pose().popMatrix();

        int textY = top + 78;
        graphics.text(this.font, Component.translatable("tflcore.menu.tagline1").getString(),
                brandX, textY, theme.muted(), false);
        graphics.text(this.font, Component.translatable("tflcore.menu.tagline2").getString(),
                brandX, textY + 12, theme.muted(), false);

        int lineY = textY + 28;
        graphics.fill(brandX, lineY, brandX + Math.min(brandWidth, 190), lineY + 1, theme.border());

        int statsY = lineY + 10;
        String[] labels = {
                Component.translatable("tflcore.menu.stat.profile").getString(),
                Component.translatable("tflcore.menu.stat.modules").getString(),
                Component.translatable("tflcore.menu.stat.hud").getString(),
                Component.translatable("tflcore.menu.stat.theme").getString()
        };
        String[] values = {
                profileName(TflCoreClient.profile()).toUpperCase(Locale.ROOT),
                String.valueOf(TflCoreClient.modules().enabledCount()),
                TflCoreClient.hud().enabledCount() + " " + Component.translatable("tflcore.menu.widgets").getString(),
                settings.ui.theme
        };
        for (int i = 0; i < labels.length; i++) {
            int y = statsY + i * 15;
            graphics.text(this.font, labels[i], brandX, y, theme.muted(), false);
            TflDraw.rightAligned(graphics, this.font, values[i], brandX + Math.min(brandWidth, 210), y,
                    i == 0 ? theme.accent() : theme.text());
        }
    }

    private void drawFooter(GuiGraphicsExtractor graphics, TflTheme theme, TflSettings settings) {
        int bottom = this.height - 16;
        graphics.text(this.font, "TFL CORE  " + BuildInfo.VERSION, 12, bottom, theme.muted(), false);
        graphics.text(this.font, "Minecraft 26.3  |  Fabric", 12, bottom - 12, theme.muted(), false);
        TflDraw.rightAligned(graphics, this.font,
                Component.translatable("tflcore.menu.hints").getString(),
                this.width - 12, bottom, theme.muted());

        String warning = null;
        int color = theme.warning();
        if (TflCoreClient.settings().isReadOnly()) {
            warning = Component.translatable("tflcore.menu.status.read_only").getString();
            color = theme.danger();
        } else if (TflCoreClient.settings().isCorrupt()) {
            warning = Component.translatable("tflcore.menu.status.corrupt").getString();
            color = theme.danger();
        } else if (TflCoreClient.settings().wasMigrated()) {
            warning = Component.translatable("tflcore.menu.status.migrated").getString();
        }
        if (warning != null) {
            graphics.text(this.font, warning, 12, bottom - 26, color, false);
        }

        if (!status.isEmpty() && UiClock.seconds() < statusUntil) {
            TflDraw.rightAligned(graphics, this.font, status, this.width - 12, bottom - 26, theme.success());
        } else if (!status.isEmpty() && UiClock.seconds() >= statusUntil) {
            status = "";
        }
    }

    @Override
    public boolean shouldCloseOnEsc() {
        return false;
    }

    @Override
    public void onClose() {
        this.minecraft.gui.setScreen(new TitleScreen());
    }
}
