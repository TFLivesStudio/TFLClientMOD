package dev.theredstonee.trsclient.ornithe.mixin;

import dev.theredstonee.trsclient.core.ui.Theme;
import dev.theredstonee.trsclient.core.ui.menus.MenuSkin;
import dev.theredstonee.trsclient.ornithe.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.widget.ButtonWidget;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ButtonWidget.class)
public abstract class ButtonWidgetMixin {
  @Shadow protected int width, height;
  @Shadow public int x, y;
  @Shadow public boolean active, visible;
  @Shadow protected boolean hovered;
  @Shadow public String message;

  @Inject(method = "render", at = @At("HEAD"), cancellable = true)
  private void tfl$render(Minecraft mc, int mx, int my, CallbackInfo ci) {
    var c = OrnitheClient.get();
    if (c == null
        || !c.ready
        || c.navigation == null
        || dev.theredstonee.trsclient.core.product.Product.safeMode()
        || mc.screen == null) return;
    if (visible) {
      hovered = mx >= x && mx < x + width && my >= y && my < y + height;
      var canvas = c.canvas;
      canvas.begin(c.window().getWidth(), c.window().getHeight(), c.window().getScale());
      try {
        MenuSkin.button(canvas, x, y, width, height, hovered, active);
        canvas.text(
            canvas.clip(message, width - 6),
            x + (width - canvas.textWidth(canvas.clip(message, width - 6))) / 2,
            y + (height - 10) / 2,
            active ? Theme.get().text : Theme.get().textDim,
            false);
      } finally {
        canvas.end();
      }
    }
    ci.cancel();
  }
}
