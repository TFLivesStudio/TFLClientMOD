package dev.theredstonee.trsclient.ornithe.mixin;

import dev.theredstonee.trsclient.ornithe.OrnitheClient;
import net.minecraft.client.gui.overlay.PlayerTabOverlay;
import net.minecraft.client.network.PlayerInfo;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(PlayerTabOverlay.class)
public abstract class TabPingMixin {
  @Inject(method = "renderPing", at = @At("HEAD"), cancellable = true)
  private void tfl$ping(int width, int x, int y, PlayerInfo info, CallbackInfo ci) {
    var c = OrnitheClient.get();
    if (c == null || !c.ready || !c.capabilities.active(c.modules.qol.tabPing)) return;
    int ping = info.getPing();
    String label = ping < 0 ? "?" : Integer.toString(ping);
    int color =
        !c.modules.qol.tabPingColors.get()
            ? 0xFFFFFFFF
            : ping < 0
                ? 0xFFAAAAAA
                : ping < 150
                    ? 0xFF55FF55
                    : ping < 300 ? 0xFFFFFF55 : ping < 600 ? 0xFFFFAA00 : 0xFFFF5555;
    var win = c.window();
    c.canvas.begin(win.getWidth(), win.getHeight(), win.getScale());
    try {
      c.canvas.push();
      c.canvas.translate(x + width - c.canvas.textWidth(label) * .75f, y + 1);
      c.canvas.scale(.75f);
      c.canvas.text(label, 0, 0, color, true);
      c.canvas.pop();
    } finally {
      c.canvas.end();
    }
    ci.cancel();
  }
}
