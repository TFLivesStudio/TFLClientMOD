package dev.theredstonee.trsclient.ornithe.mixin;

import dev.theredstonee.trsclient.ornithe.OrnitheClient;
import net.minecraft.client.gui.GameGui;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.*;

@Mixin(GameGui.class)
public abstract class GameGuiMixin {
  @Inject(method = "render", at = @At("HEAD"))
  private void tfl$start(float delta, CallbackInfo ci) {
    OrnitheClient c = OrnitheClient.get();
    if (c != null) c.crosshairVisible = false;
  }

  @Inject(method = "hasCrosshair", at = @At("RETURN"), cancellable = true)
  private void tfl$crosshair(CallbackInfoReturnable<Boolean> cir) {
    OrnitheClient c = OrnitheClient.get();
    if (c != null && c.ready) {
      c.crosshairVisible = cir.getReturnValue();
      if (c.modules.crosshair.isEnabled()) cir.setReturnValue(false);
    }
  }

  @Inject(method = "render", at = @At("TAIL"))
  private void tfl$hud(float delta, CallbackInfo ci) {
    OrnitheClient c = OrnitheClient.get();
    if (c != null) c.drawHud(delta);
  }
}
