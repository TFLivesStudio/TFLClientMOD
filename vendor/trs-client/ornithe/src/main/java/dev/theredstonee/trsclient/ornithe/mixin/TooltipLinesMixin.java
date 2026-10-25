package dev.theredstonee.trsclient.ornithe.mixin;

import dev.theredstonee.trsclient.ornithe.OrnitheClient;
import net.minecraft.entity.living.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ItemStack.class)
public abstract class TooltipLinesMixin {
  @Inject(method = "getTooltip", at = @At("RETURN"))
  private void tfl$lines(
      PlayerEntity player, boolean advanced, CallbackInfoReturnable<java.util.List<String>> cir) {
    var c = OrnitheClient.get();
    if (c != null && c.ready) c.comfort.lines((ItemStack) (Object) this, cir.getReturnValue());
  }
}
