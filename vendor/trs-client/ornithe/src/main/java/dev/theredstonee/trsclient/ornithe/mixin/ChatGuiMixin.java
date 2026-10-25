package dev.theredstonee.trsclient.ornithe.mixin;

import dev.theredstonee.trsclient.ornithe.OrnitheClient;
import net.minecraft.client.gui.chat.ChatGui;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ChatGui.class)
public abstract class ChatGuiMixin {
  @Inject(method = "addMessage(Lnet/minecraft/text/Text;)V", at = @At("HEAD"), cancellable = true)
  private void tfl$message(Text text, CallbackInfo ci) {
    var c = OrnitheClient.get();
    if (c != null && c.ready && c.chat.receive((ChatGui) (Object) this, text)) ci.cancel();
  }

  @ModifyConstant(
      method = "addMessage(Lnet/minecraft/text/Text;IIZ)V",
      constant = @Constant(intValue = 100))
  private int tfl$history(int vanilla) {
    var c = OrnitheClient.get();
    return c != null && c.ready ? c.chat.historyLimit() : vanilla;
  }

  @Inject(method = "clear", at = @At("TAIL"))
  private void tfl$clear(CallbackInfo ci) {
    var c = OrnitheClient.get();
    if (c != null && c.ready) {
      c.chat.reset();
      c.heads.clear();
    }
  }
}
