package dev.theredstonee.trsclient.ornithe.dev.mixin;
import dev.theredstonee.trsclient.ornithe.dev.FrameMetrics;
import net.minecraft.client.render.GameRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
@Mixin(GameRenderer.class)
public abstract class FrameMetricsMixin {
    @Inject(method="render(FJ)V",at=@At("HEAD"))
    private void tfl$actualFrame(float delta,long deadline,CallbackInfo ci){FrameMetrics.frame();}
}
