package dev.theredstonee.trsclient.ornithe.mixin;

import dev.theredstonee.trsclient.ornithe.OrnitheClient;
import net.minecraft.client.render.entity.LivingEntityRenderer;
import net.minecraft.entity.living.LivingEntity;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(LivingEntityRenderer.class)
public abstract class HitColorMixin {
  @Shadow protected java.nio.FloatBuffer tintBuffer;

  @Inject(
      method = "setupOverlayColor(Lnet/minecraft/entity/living/LivingEntity;FZ)Z",
      at =
          @At(
              value = "INVOKE",
              target = "Lorg/lwjgl/opengl/GL11;glTexEnv(IILjava/nio/FloatBuffer;)V"))
  private void tfl$tint(
      LivingEntity entity, float partial, boolean combine, CallbackInfoReturnable<Boolean> cir) {
    var c = OrnitheClient.get();
    if (c == null
        || !c.ready
        || !c.capabilities.active(c.modules.hitColor)
        || (entity.damagedTimer <= 0 && entity.deathTicks <= 0)) return;
    int rgb = c.modules.hitColorColor.rgb();
    tintBuffer.put(0, (rgb >> 16 & 255) / 255f);
    tintBuffer.put(1, (rgb >> 8 & 255) / 255f);
    tintBuffer.put(2, (rgb & 255) / 255f);
    tintBuffer.put(3, (float) c.modules.hitColorOpacity.get() / 100f);
  }
}
