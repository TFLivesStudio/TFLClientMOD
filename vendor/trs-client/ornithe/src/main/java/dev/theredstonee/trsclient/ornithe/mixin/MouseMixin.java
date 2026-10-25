package dev.theredstonee.trsclient.ornithe.mixin;

import dev.theredstonee.trsclient.ornithe.OrnitheClient;
import net.minecraft.client.Mouse;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Mouse.class)
public abstract class MouseMixin {
  @Shadow public int x;
  @Shadow public int y;
  @Unique private double tfl$carryX, tfl$carryY;

  @Inject(method = "tick", at = @At("TAIL"))
  private void tfl$mouse(CallbackInfo ci) {
    var c = OrnitheClient.get();
    if (c != null && c.ready) {
      double divisor = c.zoom.mouseDivisor(c.modules.zoomSlowMouse.get());
      if (divisor > 1) {
        double dx = x / divisor + tfl$carryX, dy = y / divisor + tfl$carryY;
        x = (int) dx;
        y = (int) dy;
        tfl$carryX = dx - x;
        tfl$carryY = dy - y;
      } else {
        tfl$carryX = tfl$carryY = 0;
      }
    }
  }
}
