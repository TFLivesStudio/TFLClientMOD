package dev.theredstonee.trsclient.ornithe.mixin;

import dev.theredstonee.trsclient.ornithe.OrnitheClient;
import net.minecraft.client.entity.living.player.Input;
import net.minecraft.client.entity.living.player.KeyboardInput;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(KeyboardInput.class)
public abstract class CinematicInputMixin extends Input {
  @Inject(method = "tick", at = @At("TAIL"))
  private void tfl$freeze(CallbackInfo ci) {
    var c = OrnitheClient.get();
    if (c != null && c.ready && c.cinematic.detached()) {
      movementSideways = movementForward = 0;
      jumping = sneaking = false;
    }
  }
}
