package dev.theredstonee.trsclient.ornithe.mixin;

import dev.theredstonee.trsclient.ornithe.OrnitheClient;
import net.minecraft.client.render.world.WorldRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.invoke.arg.Args;

@Mixin(WorldRenderer.class)
public abstract class WorldDetailsMixin {
  @Inject(method = "renderSky", at = @At("HEAD"), cancellable = true)
  private void tfl$sky(float partial, int pass, CallbackInfo ci) {
    var c = OrnitheClient.get();
    if (c != null
        && c.ready
        && c.capabilities.active(c.modules.worldDetails)
        && c.modules.detailNoSky.get()) ci.cancel();
  }

  // Preserve the original getStarBrightness invocation for Lucerna's render-phase hook.
  // A @Redirect replaces that invocation entirely and can prevent other mixins from finding it.
  @com.llamalad7.mixinextras.injector.ModifyExpressionValue(
      method = "renderSky",
      at =
          @At(
              value = "INVOKE",
              target = "Lnet/minecraft/client/world/ClientWorld;getStarBrightness(F)F"))
  private float tfl$stars(float original) {
    var c = OrnitheClient.get();
    return c != null
            && c.ready
            && c.capabilities.active(c.modules.worldDetails)
            && c.modules.detailNoStars.get()
        ? 0f
        : original;
  }

  @ModifyArgs(
      method = "renderBlockOutline",
      at =
          @At(
              value = "INVOKE",
              target = "Lnet/minecraft/client/render/platform/GlStateManager;color4f(FFFF)V"))
  private void tfl$outline(Args args) {
    var c = OrnitheClient.get();
    if (c == null || !c.ready || !c.capabilities.active(c.modules.blockOutline)) return;
    int rgb = c.modules.blockOutlineColor.rgb();
    args.set(0, (rgb >> 16 & 255) / 255f);
    args.set(1, (rgb >> 8 & 255) / 255f);
    args.set(2, (rgb & 255) / 255f);
    args.set(3, (float) c.modules.blockOutlineOpacity.get() / 100f);
  }

  @ModifyArg(
      method = "renderBlockOutline",
      at = @At(value = "INVOKE", target = "Lorg/lwjgl/opengl/GL11;glLineWidth(F)V"),
      index = 0)
  private float tfl$width(float width) {
    var c = OrnitheClient.get();
    return c != null && c.ready && c.capabilities.active(c.modules.blockOutline)
        ? (float) c.modules.blockOutlineWidth.get()
        : width;
  }
}
