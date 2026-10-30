package dev.theredstonee.trsclient.ornithe.mixin;

import dev.theredstonee.trsclient.core.ui.menus.*;
import dev.theredstonee.trsclient.ornithe.OrnitheClient;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.spongepowered.asm.mixin.injection.invoke.arg.Args;

@Mixin(net.minecraft.client.gui.chat.ChatGui.class)
public abstract class PauseChatMixin {
  @Inject(method = "render", at = @At("HEAD"), cancellable = true)
  private void tfl$screenChat(
      int ticks, org.spongepowered.asm.mixin.injection.callback.CallbackInfo ci) {
    var c = OrnitheClient.get();
    if (c != null
        && c.mc.screen instanceof dev.theredstonee.trsclient.ornithe.PauseChatScreen pause
        && !pause.drawingChat) ci.cancel();
  }

  @Shadow
  public abstract float getChatScale();

  @Unique
  private boolean tfl$pause() {
    var c = OrnitheClient.get();
    return c != null && c.mc.screen instanceof PauseOverlay p && p.pauseChatLayout();
  }

  @Inject(method = "getWidth()I", at = @At("RETURN"), cancellable = true)
  private void tfl$width(CallbackInfoReturnable<Integer> result) {
    if (tfl$pause())
      result.setReturnValue(
          Math.min(
              result.getReturnValue(),
              PauseLayout.chatWidth(OrnitheClient.get().window().getWidth())));
  }

  @ModifyArgs(
      method = "render",
      at =
          @At(
              value = "INVOKE",
              target = "Lnet/minecraft/client/render/platform/GlStateManager;translatef(FFF)V"))
  private void tfl$origin(Args args) {
    if (tfl$pause())
      args.set(0, (float) PauseLayout.chatLeft(OrnitheClient.get().window().getWidth()));
  }

  @ModifyVariable(method = "getMessageAt", at = @At("HEAD"), argsOnly = true, ordinal = 0)
  private int tfl$hitX(int x) {
    return tfl$pause()
        ? x
            - (PauseLayout.chatLeft(OrnitheClient.get().window().getWidth()) - 2)
                * OrnitheClient.get().window().getScale()
        : x;
  }

  @Inject(method = "getVisibleLineCount", at = @At("RETURN"), cancellable = true)
  private void tfl$pauseLines(CallbackInfoReturnable<Integer> result) {
    if (tfl$pause()) {
      var c = OrnitheClient.get();
      result.setReturnValue(
          Math.min(
              result.getReturnValue(),
              Math.max(
                  1, (int) (c.window().getHeight() * .32 / (9 * Math.max(.1, getChatScale()))))));
    }
  }
}
