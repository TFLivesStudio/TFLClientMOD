package dev.theredstonee.trsclient.ornithe.mixin;

import dev.theredstonee.trsclient.ornithe.OrnitheClient;
import net.minecraft.client.network.handler.ClientPlayNetworkHandler;
import net.minecraft.network.packet.s2c.play.ChatMessageS2CPacket;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ClientPlayNetworkHandler.class)
public abstract class ChatNetworkMixin {
  @Inject(method = "handleChatMessage", at = @At("TAIL"))
  private void tfl$received(ChatMessageS2CPacket packet, CallbackInfo ci) {
    var c = OrnitheClient.get();
    if (c != null && c.ready) c.automation.receive(packet);
  }
}
