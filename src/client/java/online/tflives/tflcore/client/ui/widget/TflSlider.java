package online.tflives.tflcore.client.ui.widget;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import online.tflives.tflcore.client.ui.TflDraw;
import online.tflives.tflcore.client.ui.TflTheme;

import java.util.function.IntConsumer;
import java.util.function.IntSupplier;
import java.util.function.Supplier;

/** Integer slider with keyboard support and live value readout. */
public class TflSlider extends AbstractWidget {
    private final IntSupplier getter;
    private final IntConsumer setter;
    private final int min;
    private final int max;
    private final int step;
    private final String suffix;
    private final Supplier<TflTheme> themeSupplier;
    private boolean dragging;

    public TflSlider(int x, int y, int width, int height, Component message, int min, int max, int step,
                     String suffix, IntSupplier getter, IntConsumer setter, Supplier<TflTheme> themeSupplier) {
        super(x, y, width, height, message);
        this.min = min;
        this.max = Math.max(min + 1, max);
        this.step = Math.max(1, step);
        this.suffix = suffix == null ? "" : suffix;
        this.getter = getter;
        this.setter = setter;
        this.themeSupplier = themeSupplier;
    }

    @Override
    protected void extractWidgetRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta) {
        TflTheme theme = themeSupplier.get();
        boolean focused = isFocused();
        int value = Math.max(min, Math.min(getter.getAsInt(), max));
        float ratio = (float) (value - min) / (float) (max - min);

        graphics.fill(getX(), getY(), getRight(), getBottom(),
                isHovered() || focused ? theme.surfaceHover() : theme.surface());
        graphics.outline(getX(), getY(), getWidth(), getHeight(), focused ? theme.borderStrong() : theme.border());
        graphics.fill(getX(), getY(), getX() + 2, getBottom(), theme.accent());

        Minecraft minecraft = Minecraft.getInstance();
        String label = TflDraw.trim(minecraft.font, getMessage().getString(), getWidth() - 84);
        int textY = getY() + 5;
        graphics.text(minecraft.font, label, getX() + 9, textY, theme.text(), false);
        String readout = value + suffix;
        TflDraw.rightAligned(graphics, minecraft.font, readout, getRight() - 8, textY, theme.accent());

        int trackX = getX() + 9;
        int trackY = getBottom() - 6;
        int trackWidth = getWidth() - 18;
        graphics.fill(trackX, trackY, trackX + trackWidth, trackY + 2, theme.accentGlow());
        int filled = Math.round(trackWidth * ratio);
        if (filled > 0) {
            graphics.fill(trackX, trackY, trackX + filled, trackY + 2, theme.accent());
        }
        int knobX = trackX + filled;
        graphics.fill(knobX - 2, trackY - 2, knobX + 2, trackY + 4, theme.accent());
    }

    @Override
    public void onClick(MouseButtonEvent event, boolean doubleClick) {
        if (!active) return;
        dragging = true;
        setFromMouse(event.x());
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        if (isValidClickButton(event.buttonInfo()) && isMouseOver(event.x(), event.y())) {
            setFocused(true);
            onClick(event, doubleClick);
            return true;
        }
        return false;
    }

    @Override
    public boolean mouseReleased(MouseButtonEvent event) {
        dragging = false;
        return super.mouseReleased(event);
    }

    @Override
    public boolean mouseDragged(MouseButtonEvent event, double dragX, double dragY) {
        if (dragging && active) {
            setFromMouse(event.x());
            return true;
        }
        return super.mouseDragged(event, dragX, dragY);
    }

    private void setFromMouse(double mouseX) {
        int trackX = getX() + 9;
        int trackWidth = Math.max(1, getWidth() - 18);
        float ratio = (float) ((mouseX - trackX) / trackWidth);
        ratio = Math.max(0F, Math.min(ratio, 1F));
        int raw = Math.round(min + ratio * (max - min));
        int snapped = Math.round((float) (raw - min) / step) * step + min;
        setter.accept(Math.max(min, Math.min(snapped, max)));
    }

    @Override
    public boolean keyPressed(KeyEvent event) {
        if (!active) return false;
        int key = event.key();
        if (key == InputConstants.KEY_LEFT) {
            setter.accept(Math.max(min, getter.getAsInt() - step));
            return true;
        }
        if (key == InputConstants.KEY_RIGHT) {
            setter.accept(Math.min(max, getter.getAsInt() + step));
            return true;
        }
        if (key == InputConstants.KEY_HOME) {
            setter.accept(min);
            return true;
        }
        if (key == InputConstants.KEY_END) {
            setter.accept(max);
            return true;
        }
        return super.keyPressed(event);
    }

    @Override
    protected void updateWidgetNarration(NarrationElementOutput output) {
        defaultButtonNarrationText(output);
    }
}
