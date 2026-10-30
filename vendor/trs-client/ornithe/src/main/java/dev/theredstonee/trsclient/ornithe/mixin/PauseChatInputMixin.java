package dev.theredstonee.trsclient.ornithe.mixin;

import dev.theredstonee.trsclient.ornithe.*;
import org.lwjgl.input.Mouse;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;

@Mixin(net.minecraft.client.gui.screen.game.ChatScreen.class)
public abstract class PauseChatInputMixin {
  @Redirect(
      method = "handleMouse",
      at = @At(value = "INVOKE", target = "Lorg/lwjgl/input/Mouse;getEventDWheel()I"))
  private int tfl$wheel() {
    int wheel = Mouse.getEventDWheel();
    var c = OrnitheClient.get();
    if (wheel != 0 && (Object) this instanceof PauseChatScreen pause) {
      int w = c.window().getWidth(), h = c.window().getHeight();
      int x = Mouse.getEventX() * w / Math.max(1, c.mc.width),
          y = h - Mouse.getEventY() * h / Math.max(1, c.mc.height) - 1;
      if (x < w / 2) {
        pause.actions.inputScroll(x, y, Math.signum(wheel));
        return 0;
      }
    }
    return wheel;
  }

  @ModifyArgs(
      method = "render",
      at =
          @At(
              value = "INVOKE",
              target = "Lnet/minecraft/client/gui/screen/game/ChatScreen;fill(IIIII)V"))
  private void tfl$inputBackground(org.spongepowered.asm.mixin.injection.invoke.arg.Args args) {
    if ((Object) this instanceof PauseChatScreen) {
      int width = OrnitheClient.get().window().getWidth();
      args.set(0, dev.theredstonee.trsclient.core.ui.menus.PauseLayout.chatLeft(width));
      args.set(2, width - 10);
    }
  }
}
