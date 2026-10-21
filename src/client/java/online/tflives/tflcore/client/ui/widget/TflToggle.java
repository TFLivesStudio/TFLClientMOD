package online.tflives.tflcore.client.ui.widget;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import online.tflives.tflcore.client.ui.TflDraw;
import online.tflives.tflcore.client.ui.TflMotion;
import online.tflives.tflcore.client.ui.TflTheme;

import java.util.function.BooleanSupplier;
import java.util.function.Consumer;
import java.util.function.Supplier;

/** Accessible on/off row with an animated switch. */
public class TflToggle extends AbstractWidget {
    private final BooleanSupplier state;
    private final Consumer<Boolean> onChange;
    private final Supplier<TflTheme> themeSupplier;
    private float hoverAmount;

    public TflToggle(int x, int y, int width, int height, Component message, BooleanSupplier state,
                     Consumer<Boolean> onChange, Supplier<TflTheme> themeSupplier) {
        super(x, y, width, height, message);
        this.state = state;
        this.onChange = onChange;
        this.themeSupplier = themeSupplier;
    }

    @Override
    protected void extractWidgetRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta) {
        TflTheme theme = themeSupplier.get();
        boolean on = state.getAsBoolean();
        boolean focused = isFocused();
        float step = TflMotion.step(delta);
        hoverAmount = TflDraw.clamp(hoverAmount + (isHovered() ? step : -step), 0F, 1F);
        float eased = TflTheme.ease(hoverAmount);

        graphics.fill(getX(), getY(), getRight(), getBottom(),
                TflTheme.blend(theme.surface(), theme.surfaceHover(), eased * 0.8F));
        graphics.outline(getX(), getY(), getWidth(), getHeight(), focused ? theme.borderStrong() : theme.border());
        if (on) {
            graphics.fill(getX(), getY(), getX() + 2, getBottom(), theme.accent());
        }

        Minecraft minecraft = Minecraft.getInstance();
        String label = TflDraw.trim(minecraft.font, getMessage().getString(), getWidth() - 64);
        int textY = getY() + (getHeight() - minecraft.font.lineHeight) / 2 + 1;
        graphics.text(minecraft.font, label, getX() + 9, textY, on ? theme.text() : theme.muted(), false);

        int trackWidth = 30;
        int trackHeight = 12;
        int trackX = getRight() - trackWidth - 8;
        int trackY = getY() + (getHeight() - trackHeight) / 2;
        graphics.fill(trackX, trackY, trackX + trackWidth, trackY + trackHeight,
                on ? theme.accentSoft() : TflTheme.withAlpha(theme.muted(), 0.18F));
        graphics.outline(trackX, trackY, trackWidth, trackHeight, on ? theme.accent() : theme.border());
        int knobSize = 8;
        int knobX = on ? trackX + trackWidth - knobSize - 2 : trackX + 2;
        graphics.fill(knobX, trackY + 2, knobX + knobSize, trackY + 2 + knobSize,
                on ? theme.accent() : theme.muted());
    }

    @Override
    public void onClick(MouseButtonEvent event, boolean doubleClick) {
        if (!active) return;
        playDownSound(Minecraft.getInstance().getSoundManager());
        onChange.accept(!state.getAsBoolean());
    }

    @Override
    public boolean keyPressed(KeyEvent event) {
        if (!active) return false;
        if (event.key() == com.mojang.blaze3d.platform.InputConstants.KEY_RETURN
                || event.key() == com.mojang.blaze3d.platform.InputConstants.KEY_SPACE) {
            onChange.accept(!state.getAsBoolean());
            return true;
        }
        return super.keyPressed(event);
    }

    @Override
    protected void updateWidgetNarration(NarrationElementOutput output) {
        defaultButtonNarrationText(output);
    }
}
