package dev.theredstonee.trsclient.ornithe.mixin;

import dev.theredstonee.trsclient.ornithe.OrnitheClient;
import net.minecraft.entity.Entity;
import net.minecraft.entity.living.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LivingEntity.class)
public abstract class LivingEntityMixin {
  @Inject(method = "doEvent", at = @At("HEAD"))
  private void tfl$event(byte event, CallbackInfo ci) {
    var c = OrnitheClient.get();
    if (c != null && c.ready && c.pvp != null) c.pvp.event((Entity) (Object) this, event);
  }
}
