package dev.theredstonee.trsclient.ornithe.mixin;

import dev.theredstonee.trsclient.core.connect.FastConnect;
import net.minecraft.network.Connection;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Connection.class)
public abstract class FastConnectMixin {
  @Inject(method = "connect", at = @At("HEAD"))
  private static void tfl$race(
      java.net.InetAddress address,
      int port,
      boolean epoll,
      CallbackInfoReturnable<Connection> cir) {
    FastConnect.beforeConnect(new java.net.InetSocketAddress(address, port));
  }

  @ModifyArg(
      method = "connect",
      at =
          @At(
              value = "INVOKE",
              target =
                  "Lio/netty/bootstrap/Bootstrap;channel(Ljava/lang/Class;)Lio/netty/bootstrap/AbstractBootstrap;",
              remap = false),
      index = 0)
  private static Class<?> tfl$channel(Class<?> original) {
    return FastConnect.channelClass(original);
  }

  @ModifyArg(
      method = "connect",
      at =
          @At(
              value = "INVOKE",
              target =
                  "Lio/netty/bootstrap/Bootstrap;connect(Ljava/net/InetAddress;I)Lio/netty/channel/ChannelFuture;",
              remap = false),
      index = 0)
  private static java.net.InetAddress tfl$target(java.net.InetAddress original) {
    return FastConnect.connectAddress(original);
  }

  @Inject(method = "connect", at = @At("RETURN"))
  private static void tfl$done(
      java.net.InetAddress address,
      int port,
      boolean epoll,
      CallbackInfoReturnable<Connection> cir) {
    FastConnect.afterConnect();
  }
}
