package dev.theredstonee.trsclient.ornithe.mixin;

import dev.theredstonee.trsclient.ornithe.OrnitheClient;
import net.minecraft.client.render.ItemInHandRenderer;
import net.minecraft.client.render.platform.GlStateManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ItemInHandRenderer.class)
public abstract class ItemInHandMixin {
  @Inject(
      method = {"renderInFirstPerson", "renderScreenEffects"},
      at = @At("HEAD"),
      cancellable = true)
  private void tfl$detached(float partial, CallbackInfo ci) {
    var c = OrnitheClient.get();
    if (c != null && c.ready && c.cinematic.detached()) ci.cancel();
  }

  @Inject(method = "renderOnFireEffect", at = @At("HEAD"))
  private void tfl$fire(float partial, CallbackInfo ci) {
    GlStateManager.pushMatrix();
    var c = OrnitheClient.get();
    if (c != null && c.ready && c.capabilities.active(c.modules.lowFire))
      GlStateManager.translatef(0, -(float) c.modules.lowFireHeight.get(), 0);
  }

  @Inject(method = "renderOnFireEffect", at = @At("RETURN"))
  private void tfl$restoreFire(float partial, CallbackInfo ci) {
    GlStateManager.popMatrix();
  }

  @ModifyVariable(
      method = "applyFirstPersonTransform",
      at = @At("HEAD"),
      argsOnly = true,
      ordinal = 0)
  private float tfl$equip(float equip) {
    var c = OrnitheClient.get();
    return c != null
            && c.ready
            && c.capabilities.active(c.modules.oldAnimations)
            && c.modules.oldAnimationsNoDip.get()
        ? 0
        : equip;
  }

  @ModifyVariable(
      method = "applyFirstPersonTransform",
      at = @At("HEAD"),
      argsOnly = true,
      ordinal = 1)
  private float tfl$swing(float swing) {
    var c = OrnitheClient.get();
    return c != null
            && c.ready
            && c.mc.player != null
            && c.mc.player.isUsingItem()
            && c.capabilities.active(c.modules.oldAnimations)
            && c.modules.oldAnimationsBlockHit.get()
        ? c.mc.player.getAttackAnimationProgress(c.hudPartial)
        : swing;
  }
}
