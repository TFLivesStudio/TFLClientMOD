package online.tflives.tflcore.client.hud.widget;

import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import online.tflives.tflcore.client.config.TflSettings;
import online.tflives.tflcore.client.config.WidgetPlacement;
import online.tflives.tflcore.client.hud.HudWidget;
import online.tflives.tflcore.client.ui.TflTheme;

/** WASD + space + shift key display, driven by the vanilla key states (no synthetic input). */
public final class KeystrokesWidget extends HudWidget {
    private static final int KEY = 16;
    private static final int GAP = 2;

    public KeystrokesWidget() {
        super("keystrokes", false, WidgetPlacement.BOTTOM_RIGHT, 6, 6);
    }

    @Override
    protected void collect(Minecraft client, TflSettings settings) {
        contentWidth = KEY * 3 + GAP * 2;
        contentHeight = KEY * 3 + GAP * 2 + 10;
    }

    @Override
    public void render(GuiGraphicsExtractor graphics, Minecraft client, TflTheme theme, TflSettings settings,
                       WidgetPlacement placement, int width, int height) {
        int x = 0;
        int y = 0;
        key(graphics, client, theme, client.options.keyUp, "W", x + KEY + GAP, y);

        y += KEY + GAP;
        key(graphics, client, theme, client.options.keyLeft, "A", x, y);
        key(graphics, client, theme, client.options.keyDown, "S", x + KEY + GAP, y);
        key(graphics, client, theme, client.options.keyRight, "D", x + (KEY + GAP) * 2, y);

        y += KEY + GAP;
        int spaceWidth = KEY * 3 + GAP * 2;
        graphics.fill(x, y, x + spaceWidth, y + 8, client.options.keyJump.isDown() ? theme.accentSoft() : theme.surface());
        graphics.outline(x, y, spaceWidth, 8, theme.border());
        TflTheme finalTheme = theme;
        graphics.centeredText(client.font, "_", x + spaceWidth / 2, y + 1,
                client.options.keyJump.isDown() ? finalTheme.accent() : finalTheme.muted());

        y += 10;
        int shiftWidth = (spaceWidth - GAP) / 2;
        graphics.fill(x, y, x + shiftWidth, y + 8,
                client.options.keyShift.isDown() ? theme.accentSoft() : theme.surface());
        graphics.outline(x, y, shiftWidth, 8, theme.border());
        graphics.centeredText(client.font, "SHIFT", x + shiftWidth / 2, y + 1,
                client.options.keyShift.isDown() ? theme.accent() : theme.muted());
        int sprintX = x + shiftWidth + GAP;
        graphics.fill(sprintX, y, x + spaceWidth, y + 8,
                client.options.keySprint.isDown() ? theme.accentSoft() : theme.surface());
        graphics.outline(sprintX, y, spaceWidth - shiftWidth - GAP, 8, theme.border());
        graphics.centeredText(client.font, "SPR", sprintX + (spaceWidth - shiftWidth - GAP) / 2, y + 1,
                client.options.keySprint.isDown() ? theme.accent() : theme.muted());
    }

    private void key(GuiGraphicsExtractor graphics, Minecraft client, TflTheme theme, KeyMapping mapping,
                     String label, int x, int y) {
        boolean down = mapping.isDown();
        graphics.fill(x, y, x + KEY, y + KEY, down ? theme.accentSoft() : theme.surface());
        graphics.outline(x, y, KEY, KEY, down ? theme.accent() : theme.border());
        int color = down ? theme.accent() : theme.muted();
        graphics.centeredText(client.font, label, x + KEY / 2, y + (KEY - client.font.lineHeight) / 2 + 1, color);
    }
}
