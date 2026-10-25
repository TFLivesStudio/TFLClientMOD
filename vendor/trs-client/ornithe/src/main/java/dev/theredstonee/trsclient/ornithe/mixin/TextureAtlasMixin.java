package dev.theredstonee.trsclient.ornithe.mixin;

import dev.theredstonee.trsclient.ornithe.OrnitheClient;
import net.minecraft.client.render.texture.TextureAtlas;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(TextureAtlas.class)
public abstract class TextureAtlasMixin {
  @Inject(method = "bindAndTick", at = @At("HEAD"), cancellable = true)
  private void tfl$animations(CallbackInfo ci) {
    var c = OrnitheClient.get();
    if (c != null
        && c.ready
        && c.capabilities.active(c.modules.worldDetails)
        && c.modules.detailNoAnimations.get()) ci.cancel();
  }
}
