package dev.theredstonee.trsclient.ornithe.mixin;

import dev.theredstonee.trsclient.ornithe.OrnitheClient;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Screen.class)
public abstract class TooltipMixin {
  @Inject(method = "renderTooltip(Lnet/minecraft/item/ItemStack;II)V", at = @At("TAIL"))
  private void tfl$card(ItemStack stack, int x, int y, CallbackInfo ci) {
    var c = OrnitheClient.get();
    if (c != null && c.ready) c.comfort.tooltip(stack, x, y);
  }
}
