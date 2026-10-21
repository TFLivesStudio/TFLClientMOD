package online.tflives.tflcore.client.hud.widget;

import net.minecraft.client.Minecraft;
import net.minecraft.world.effect.MobEffectInstance;
import online.tflives.tflcore.client.config.TflSettings;
import online.tflives.tflcore.client.config.WidgetPlacement;
import online.tflives.tflcore.client.hud.HudWidget;

import java.util.Locale;

/** Active potion effects with remaining duration, capped to keep the widget compact. */
public final class PotionsWidget extends HudWidget {
    private static final int MAX_EFFECTS = 5;

    public PotionsWidget() {
        super("potions", false, WidgetPlacement.TOP_LEFT, 6, 138);
        setRefreshIntervalNanos(500_000_000L);
    }

    @Override
    protected void collect(Minecraft client, TflSettings settings) {
        if (client.player == null) return;
        int shown = 0;
        for (MobEffectInstance effect : client.player.getActiveEffects()) {
            if (shown++ >= MAX_EFFECTS) break;
            String name = effect.getEffect().value().getDisplayName().getString();
            int level = effect.getAmplifier() + 1;
            int seconds = Math.max(0, effect.getDuration() / 20);
            line(String.format(Locale.ROOT, "%s %d  %d:%02d", name, level, seconds / 60, seconds % 60));
        }
        if (shown == 0) {
            line("NO EFFECTS");
        }
    }
}
