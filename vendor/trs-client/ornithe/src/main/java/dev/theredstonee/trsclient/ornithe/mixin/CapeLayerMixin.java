package dev.theredstonee.trsclient.ornithe.mixin;

import dev.theredstonee.trsclient.ornithe.OrnitheClient;
import net.minecraft.client.entity.living.player.ClientPlayerEntity;
import net.minecraft.client.render.entity.PlayerRenderer;
import net.minecraft.client.render.entity.layer.CapeLayer;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(CapeLayer.class)
public abstract class CapeLayerMixin {
  @Shadow @Final private PlayerRenderer parent;

  @Inject(
      method = "render(Lnet/minecraft/client/entity/living/player/ClientPlayerEntity;FFFFFFF)V",
      at = @At("HEAD"),
      cancellable = true)
  private void tfl$cape(
      ClientPlayerEntity p,
      float swing,
      float amount,
      float partial,
      float age,
      float yaw,
      float pitch,
      float scale,
      CallbackInfo ci) {
    var c = OrnitheClient.get();
    if (c != null && c.ready && c.capes.render(parent, p)) ci.cancel();
  }
}
