package dev.theredstonee.trsclient.ornithe.mixin;

import dev.theredstonee.trsclient.ornithe.OrnitheClient;
import net.minecraft.client.options.KeyBinding;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.*;

@Mixin(KeyBinding.class)
public abstract class KeyBindingMixin {
  @Inject(method = "set", at = @At("HEAD"))
  private static void tfl$keyState(int key, boolean down, CallbackInfo ci) {
    OrnitheClient c = OrnitheClient.get();
    if (c != null) c.keyState(key, down);
  }

  @Inject(method = "click", at = @At("HEAD"))
  private static void tfl$click(int key, CallbackInfo ci) {
    OrnitheClient c = OrnitheClient.get();
    if (c != null) c.click(key);
  }

  @Inject(method = "isPressed", at = @At("RETURN"), cancellable = true)
  private void tfl$sprint(CallbackInfoReturnable<Boolean> cir) {
    OrnitheClient c = OrnitheClient.get();
    if (c != null && c.sprintHeld((KeyBinding) (Object) this)) cir.setReturnValue(true);
  }
}
