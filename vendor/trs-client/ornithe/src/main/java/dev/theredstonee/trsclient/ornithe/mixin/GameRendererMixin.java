package dev.theredstonee.trsclient.ornithe.mixin;

import dev.theredstonee.trsclient.ornithe.OrnitheClient;
import net.minecraft.client.render.GameRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.*;

@Mixin(GameRenderer.class)
public abstract class GameRendererMixin {
  @Inject(method = "render", at = @At("HEAD"))
  private void tfl$frame(float delta, long time, CallbackInfo ci) {
    OrnitheClient c = OrnitheClient.get();
    if (c != null) c.frame();
  }

  @Inject(method = "getFov", at = @At("RETURN"), cancellable = true)
  private void tfl$fov(float delta, boolean changing, CallbackInfoReturnable<Float> cir) {
    OrnitheClient c = OrnitheClient.get();
    if (c != null && c.ready) cir.setReturnValue((float) (cir.getReturnValue() / c.zoom.factor()));
  }
}
