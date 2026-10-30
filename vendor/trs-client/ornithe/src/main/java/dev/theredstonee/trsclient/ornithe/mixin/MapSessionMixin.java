package dev.theredstonee.trsclient.ornithe.mixin;

import dev.theredstonee.trsclient.ornithe.OrnitheClient;
import net.minecraft.client.network.handler.ClientPlayNetworkHandler;
import net.minecraft.network.packet.s2c.play.*;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ClientPlayNetworkHandler.class)
public abstract class MapSessionMixin {
  @Inject(method = "handleLogin", at = @At("TAIL"))
  private void tfl$register(LoginS2CPacket packet, CallbackInfo ci) {
    var c = OrnitheClient.get();
    if (c == null || !c.ready || c.mc.getNetworkHandler() == null) return;
    // Standard plugin-channel registration only. No chat commands, API keys or external endpoints.
    byte[] channels =
        "xaerominimap:main\0xaeroworldmap:main".getBytes(java.nio.charset.StandardCharsets.UTF_8);
    c.mc
        .getNetworkHandler()
        .sendPacket(
            new net.minecraft.network.packet.c2s.play.CustomPayloadC2SPacket(
                "REGISTER",
                new net.minecraft.network.PacketByteBuf(
                    io.netty.buffer.Unpooled.wrappedBuffer(channels))));
  }

  @Inject(method = "handlePlayerRespawn", at = @At("TAIL"))
  private void tfl$respawn(PlayerRespawnS2CPacket packet, CallbackInfo ci) {
    var c = OrnitheClient.get();
    if (c != null && c.ready) c.mapPlatform.respawning();
  }

  @Inject(method = "handleCustomPayload", at = @At("HEAD"))
  private void tfl$world(CustomPayloadS2CPacket packet, CallbackInfo ci) {
    String channel = packet.getChannel();
    if (!"xaerominimap:main".equals(channel) && !"xaeroworldmap:main".equals(channel)) return;
    var data = packet.getData();
    if (data == null || data.readableBytes() < 5) return;
    int at = data.readerIndex();
    int kind = data.getByte(at);
    if (kind == 1) {
      var client = OrnitheClient.get();
      if (client != null && client.ready)
        client.mc.executeTask(
            () -> {
              if (client.mc.getNetworkHandler() != null) {
                var reply =
                    new net.minecraft.network.PacketByteBuf(io.netty.buffer.Unpooled.buffer(5));
                reply.writeByte(1);
                reply.writeInt(3);
                client
                    .mc
                    .getNetworkHandler()
                    .sendPacket(
                        new net.minecraft.network.packet.c2s.play.CustomPayloadC2SPacket(
                            channel, reply));
              }
            });
      return;
    }
    if (kind != 0) return;
    int id = data.getInt(at + 1);
    var c = OrnitheClient.get();
    if (c != null && c.ready) c.mc.executeTask(() -> c.mapPlatform.worldId(id));
  }
}
