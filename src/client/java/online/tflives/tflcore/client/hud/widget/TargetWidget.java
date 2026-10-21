package online.tflives.tflcore.client.hud.widget;

import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import online.tflives.tflcore.client.config.TflSettings;
import online.tflives.tflcore.client.config.WidgetPlacement;
import online.tflives.tflcore.client.hud.HudWidget;

/** Block currently under the crosshair, using only data the client already knows. */
public final class TargetWidget extends HudWidget {
    public TargetWidget() {
        super("target", false, WidgetPlacement.TOP_LEFT, 6, 160);
        setRefreshIntervalNanos(150_000_000L);
    }

    @Override
    protected void collect(Minecraft client, TflSettings settings) {
        if (client.hitResult == null || client.hitResult.getType() != HitResult.Type.BLOCK
                || !(client.hitResult instanceof BlockHitResult hit) || client.level == null) {
            line("TARGET  \u2014");
            return;
        }
        BlockPos pos = hit.getBlockPos();
        BlockState state = client.level.getBlockState(pos);
        line(titleCase(BuiltInRegistries.BLOCK.getKey(state.getBlock()).toString()));
        line(pos.getX() + "  " + pos.getY() + "  " + pos.getZ());
        if (state.getLightEmission() > 0) {
            line("LIGHT  " + state.getLightEmission());
        }
    }
}
