package dev.theredstonee.trsclient.ornithe.mixin;

import dev.theredstonee.trsclient.ornithe.OrnitheClient;
import net.minecraft.client.gui.GameGui;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.*;

@Mixin(GameGui.class)
public abstract class GameGuiMixin {
  @Inject(method = "render", at = @At("HEAD"), cancellable = true)
  private void tfl$start(float delta, CallbackInfo ci) {
    OrnitheClient c = OrnitheClient.get();
    if (c != null) {
      c.crosshairVisible = false;
      if (c.mc.screen instanceof dev.theredstonee.trsclient.core.ui.menus.PauseOverlay) {
        // GameGui normally establishes the orthographic projection used by Screen.render.
        // Keep that setup while omitting every HUD element, including custom callbacks.
        c.mc.gameRenderer.setupGuiState();
        net.minecraft.client.render.platform.GlStateManager.enableBlend();
        net.minecraft.client.render.platform.GlStateManager.blendFuncSeparate(770, 771, 1, 0);
        ci.cancel();
      }
    }
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
    dev.theredstonee.trsclient.core.performance.UiProfiler.worldFrame();
    OrnitheClient c = OrnitheClient.get();
    if (c != null) c.drawHud(delta);
  }
}
