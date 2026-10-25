package dev.theredstonee.trsclient.ornithe.mixin;

import dev.theredstonee.trsclient.ornithe.*;
import net.minecraft.client.gui.screen.Screen;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Screen.class)
public abstract class ScreenMixin {
  @Shadow public int width, height;

  @Inject(method = "renderBackground(I)V", at = @At("HEAD"), cancellable = true)
  private void tfl$background(int tint, CallbackInfo ci) {
    var c = OrnitheClient.get();
    if (c == null
        || !c.ready
        || c.navigation == null
        || dev.theredstonee.trsclient.core.product.Product.safeMode()
        || !getClass().getName().startsWith("net.minecraft.")) return;
    c.canvas.begin(width, height, c.window().getScale());
    try {
      dev.theredstonee.trsclient.core.ui.menus.MenuSkin.background(
          c.canvas, width, height, c.mc.world != null, 32, height - 22);
    } finally {
      c.canvas.end();
    }
    ci.cancel();
  }
}
