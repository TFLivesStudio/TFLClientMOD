package dev.theredstonee.trsclient.ornithe.mixin;

import dev.theredstonee.trsclient.ornithe.OrnitheClient;
import net.minecraft.client.ClientPlayerInteractionManager;
import net.minecraft.entity.Entity;
import net.minecraft.entity.living.player.PlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ClientPlayerInteractionManager.class)
public abstract class AttackMixin {
  @Inject(method = "attackEntity", at = @At("HEAD"))
  private void tfl$attack(PlayerEntity player, Entity entity, CallbackInfo ci) {
    var c = OrnitheClient.get();
    if (c != null && c.ready && c.pvp != null && player == c.mc.player) c.pvp.attack(entity);
  }
}
