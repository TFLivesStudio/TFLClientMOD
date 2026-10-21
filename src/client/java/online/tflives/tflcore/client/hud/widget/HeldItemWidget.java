package online.tflives.tflcore.client.hud.widget;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.world.item.ItemStack;
import online.tflives.tflcore.client.config.TflSettings;
import online.tflives.tflcore.client.config.WidgetPlacement;
import online.tflives.tflcore.client.hud.HudWidget;
import online.tflives.tflcore.client.ui.TflDraw;
import online.tflives.tflcore.client.ui.TflTheme;

/** Held item name, stack count and durability. */
public final class HeldItemWidget extends HudWidget {
    public HeldItemWidget() {
        super("helditem", false, WidgetPlacement.BOTTOM_RIGHT, 6, 80);
        setRefreshIntervalNanos(250_000_000L);
    }

    @Override
    protected void collect(Minecraft client, TflSettings settings) {
        contentWidth = 138;
        contentHeight = 20;
    }

    @Override
    public void render(GuiGraphicsExtractor graphics, Minecraft client, TflTheme theme, TflSettings settings,
                       WidgetPlacement placement, int width, int height) {
        if (client.player == null) return;
        ItemStack stack = client.player.getMainHandItem();
        if (stack.isEmpty()) {
            graphics.text(client.font, "EMPTY HAND", 0, 6, theme.muted(), false);
            return;
        }
        graphics.item(stack, 0, 2);
        String name = TflDraw.trim(client.font, stack.getHoverName().getString(), 100);
        graphics.text(client.font, name, 20, 1, theme.text(), false);
        String count = stack.getCount() > 1 ? " x" + stack.getCount() : "";
        graphics.text(client.font, count, 20 + client.font.width(name), 1, theme.accent(), false);
        if (stack.isDamageableItem() && stack.getMaxDamage() > 0) {
            int remaining = stack.getMaxDamage() - stack.getDamageValue();
            float ratio = remaining / (float) stack.getMaxDamage();
            TflDraw.progressBar(graphics, theme, 20, 12, 100, 3, ratio);
        }
    }
}
