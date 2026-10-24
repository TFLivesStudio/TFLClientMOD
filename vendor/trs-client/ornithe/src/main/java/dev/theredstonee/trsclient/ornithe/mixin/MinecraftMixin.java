package dev.theredstonee.trsclient.ornithe.mixin;

import dev.theredstonee.trsclient.core.product.Product;
import dev.theredstonee.trsclient.ornithe.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.menu.TitleScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Minecraft.class)
public abstract class MinecraftMixin {
  @Inject(method = "openScreen", at = @At("TAIL"))
  private void tfl$title(Screen screen, CallbackInfo ci) {
    OrnitheClient c = OrnitheClient.get();
    Minecraft mc = (Minecraft) (Object) this;
    if (c != null
        && c.ready
        && !Product.safeMode()
        && mc.screen != null
        && mc.screen.getClass() == TitleScreen.class) mc.openScreen(c.title());
  }

  @Inject(method = "reloadResources", at = @At("TAIL"))
  private void tfl$reload(CallbackInfo ci) {
    OrnitheClient c = OrnitheClient.get();
    if (c != null && c.ready) c.reload();
  }
}
