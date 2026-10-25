package dev.theredstonee.trsclient.ornithe.mixin;

import dev.theredstonee.trsclient.core.net.NetBoost;
import dev.theredstonee.trsclient.ornithe.OrnitheClient;
import io.netty.channel.ChannelHandlerContext;
import net.minecraft.network.*;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Connection.class)
public abstract class NetConnectionMixin {
  @Shadow @Final private PacketFlow flow;

  @Inject(method = "channelActive", at = @At("TAIL"))
  private void tfl$clientOnly(ChannelHandlerContext ctx, CallbackInfo ci) {
    var c = OrnitheClient.get();
    if (c != null && c.ready && flow == PacketFlow.CLIENTBOUND) NetBoost.attach(ctx.channel());
  }
}
