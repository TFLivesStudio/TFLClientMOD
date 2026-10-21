package online.tflives.tflcore.client.hud.widget;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import online.tflives.tflcore.client.config.TflSettings;
import online.tflives.tflcore.client.config.WidgetPlacement;
import online.tflives.tflcore.client.hud.HudWidget;
import online.tflives.tflcore.client.ui.TflDraw;
import online.tflives.tflcore.client.ui.TflTheme;

import java.util.Locale;

/** Armor pieces with remaining durability, read from the local player's equipment. */
public final class ArmorWidget extends HudWidget {
    private static final EquipmentSlot[] SLOTS = {
            EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET
    };
    private static final int ROW = 18;

    public ArmorWidget() {
        super("armor", false, WidgetPlacement.TOP_LEFT, 6, 28);
        setRefreshIntervalNanos(500_000_000L);
    }

    @Override
    protected void collect(Minecraft client, TflSettings settings) {
        contentWidth = 118;
        contentHeight = ROW * SLOTS.length;
    }

    @Override
    public void render(GuiGraphicsExtractor graphics, Minecraft client, TflTheme theme, TflSettings settings,
                       WidgetPlacement placement, int width, int height) {
        if (client.player == null) return;
        for (int i = 0; i < SLOTS.length; i++) {
            int y = i * ROW;
            ItemStack stack = client.player.getItemBySlot(SLOTS[i]);
            if (stack.isEmpty()) {
                graphics.text(client.font, "\u2014", 20, y + 5, theme.muted(), false);
                continue;
            }
            graphics.item(stack, 0, y);
            String name = TflDraw.trim(client.font, stack.getHoverName().getString(), 78);
            graphics.text(client.font, name, 20, y + 1, theme.text(), false);
            if (stack.isDamageableItem() && stack.getMaxDamage() > 0) {
                int remaining = stack.getMaxDamage() - stack.getDamageValue();
                float ratio = remaining / (float) stack.getMaxDamage();
                graphics.fill(20, y + 11, 98, y + 13, theme.accentGlow());
                int filled = Math.round(78 * Math.max(0F, Math.min(ratio, 1F)));
                int color = ratio > 0.5F ? theme.success() : ratio > 0.2F ? theme.warning() : theme.danger();
                graphics.fill(20, y + 11, 20 + filled, y + 13, color);
                String durability = String.format(Locale.ROOT, "%d%%", Math.round(ratio * 100F));
                TflDraw.rightAligned(graphics, client.font, durability, 116, y + 1, theme.muted());
            }
        }
    }
}
