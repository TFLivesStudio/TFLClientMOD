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

import java.util.function.Supplier;

/** Design-system button. Draws inside the modern render-state extractor, never with raw OpenGL. */
public class TflButton extends AbstractWidget {
    public enum Style {
        PRIMARY, SECONDARY, GHOST, DANGER, TAB
    }

    private final Runnable action;
    private final Style style;
    private final Supplier<TflTheme> themeSupplier;
    private boolean selected;
    private float hoverAmount;
    private String subtitle = "";
    private String trailing = "";

    public TflButton(int x, int y, int width, int height, Component message, Style style,
                     Supplier<TflTheme> themeSupplier, Runnable action) {
        super(x, y, width, height, message);
        this.style = style;
        this.themeSupplier = themeSupplier;
        this.action = action;
    }

    public TflButton subtitle(Component subtitle) {
        this.subtitle = subtitle == null ? "" : subtitle.getString();
        return this;
    }

    public TflButton trailing(String trailing) {
        this.trailing = trailing == null ? "" : trailing;
        return this;
    }

    public TflButton selected(boolean selected) {
        this.selected = selected;
        return this;
    }

    public boolean isSelected() {
        return selected;
    }

    public Style style() {
        return style;
    }

    @Override
    protected void extractWidgetRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta) {
        TflTheme theme = themeSupplier.get();
        boolean focused = isFocused();
        float step = TflMotion.step(delta);
        hoverAmount = TflDraw.clamp(hoverAmount + (isHovered() || selected ? step : -step), 0F, 1F);
        float eased = TflTheme.ease(hoverAmount);

        int background;
        int textColor;
        switch (style) {
            case PRIMARY -> {
                background = TflTheme.blend(theme.accent(), theme.surfaceActive(), 0.55F - eased * 0.25F);
                textColor = 0xFF04121F;
            }
            case DANGER -> {
                background = TflTheme.blend(theme.surface(), theme.danger(), 0.30F + eased * 0.20F);
                textColor = theme.text();
            }
            case GHOST -> {
                background = TflTheme.withAlpha(TflTheme.blend(theme.surface(), theme.accent(), 0.10F), 0.35F + eased * 0.3F);
                textColor = theme.muted();
            }
            case TAB -> {
                background = selected ? theme.accentSoft() : TflTheme.withAlpha(theme.surface(), 0.65F);
                textColor = selected ? theme.text() : theme.muted();
            }
            default -> {
                background = TflTheme.blend(theme.surface(), theme.surfaceHover(), eased);
                textColor = theme.text();
            }
        }

        graphics.fill(getX() + 1, getY() + 2, getRight() + 1, getBottom() + 2, theme.shadow());
        graphics.fill(getX(), getY(), getRight(), getBottom(), background);
        graphics.outline(getX(), getY(), getWidth(), getHeight(),
                focused ? theme.borderStrong() : theme.border());
        if (selected || style == Style.PRIMARY) {
            graphics.fill(getX(), getY(), getX() + 2, getBottom(), theme.accent());
        }
        if (eased > 0.05F && style != Style.PRIMARY) {
            graphics.fill(getX(), getY(), getRight(), getY() + 1, TflTheme.withAlpha(theme.accent(), eased * 0.7F));
        }

        Minecraft minecraft = Minecraft.getInstance();
        boolean hasSubtitle = !subtitle.isEmpty();
        int labelX = getX() + (style == Style.TAB ? 6 : 11);
        int maxWidth = getWidth() - (style == Style.TAB ? 12 : 26);
        String label = TflDraw.trim(minecraft.font, getMessage().getString(), maxWidth);
        if (hasSubtitle) {
            int baseY = getY() + Math.max(4, (getHeight() - 22) / 2);
            graphics.text(minecraft.font, label, labelX, baseY, textColor, focused);
            graphics.text(minecraft.font, TflDraw.trim(minecraft.font, subtitle, maxWidth),
                    labelX, baseY + 12, TflTheme.blend(textColor, theme.muted(), 0.45F), false);
        } else {
            int textY = getY() + (getHeight() - minecraft.font.lineHeight) / 2 + 1;
            graphics.text(minecraft.font, label, labelX, textY, textColor, focused);
        }
        if (!trailing.isEmpty()) {
            String suffix = TflDraw.trim(minecraft.font, trailing, Math.max(0, getWidth() / 3));
            TflDraw.rightAligned(graphics, minecraft.font, suffix, getRight() - 9,
                    getY() + (getHeight() - minecraft.font.lineHeight) / 2 + 1, theme.accent());
        }
        if (style == Style.TAB && selected) {
            graphics.fill(getX() + 4, getBottom() - 2, getRight() - 4, getBottom(), theme.accent());
        }
    }

    @Override
    public void onClick(MouseButtonEvent event, boolean doubleClick) {
        if (!active) return;
        playDownSound(Minecraft.getInstance().getSoundManager());
        action.run();
    }

    @Override
    public boolean keyPressed(KeyEvent event) {
        if (!active) return false;
        if (event.key() == com.mojang.blaze3d.platform.InputConstants.KEY_RETURN
                || event.key() == com.mojang.blaze3d.platform.InputConstants.KEY_SPACE
                || event.key() == com.mojang.blaze3d.platform.InputConstants.KEY_NUMPADENTER) {
            playDownSound(Minecraft.getInstance().getSoundManager());
            action.run();
            return true;
        }
        return super.keyPressed(event);
    }

    @Override
    protected void updateWidgetNarration(NarrationElementOutput output) {
        defaultButtonNarrationText(output);
    }
}
