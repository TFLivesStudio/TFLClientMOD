package dev.theredstonee.trsclient.ornithe;

import dev.theredstonee.trsclient.core.net.*;
import dev.theredstonee.trsclient.ornithe.capability.CapabilityRegistry.State;
import dev.theredstonee.trsclient.ornithe.mixin.*;
import dev.theredstonee.trsclient.ornithe.net.*;
import io.netty.channel.ChannelHandler;
import net.minecraft.network.*;

/**
 * Existing tested shared codecs/watchers; only exact Vanilla handlers are eligible for replacement.
 */
public final class OrnitheNet implements NetPlatform {
  private final OrnitheClient c;

  public OrnitheNet(OrnitheClient c) {
    this.c = c;
    dev.theredstonee.trsclient.core.connect.FastConnect.init(
        null,
        () -> c.capabilities.active(c.modules.fastConnect),
        () -> c.modules.fastConnectPreResolve.get(),
        null);
    c.capabilities.connect(
        c.modules.fastConnect,
        State.EXPERIMENTAL,
        "Shared DNS cache/prefetch and connection address racing with Vanilla handshake identity."
            + " Launcher hints and resource-pack preloads are disabled; native multi-address"
            + " acceptance remains pending.",
        java.util.List.of("preResolve"),
        "FastConnectMixin",
        "FastConnectSelectionMixin",
        "FastConnect.init(configDir=null)");
    NetBoost.init(
        this,
        () -> c.capabilities.active(c.modules.netOptimize),
        new NetBoost.Logger() {
          public void info(String s) {
            OrnitheClient.LOG.info(s);
          }

          public void warn(String s, Throwable error) {
            OrnitheClient.LOG.warn(s, error);
          }
        });
    c.capabilities.connect(
        c.modules.netOptimize,
        State.EXPERIMENTAL,
        "Shared compression/decryption adapters replace exact Vanilla handlers only."
            + " Timing/keepalive packets are observed, never delayed or changed; multiplayer native"
            + " acceptance remains pending.",
        java.util.List.of(),
        "OrnitheNet",
        "NetConnectionMixin(clientbound only)",
        "NetBoost",
        "NetCodecs");
  }

  public ChannelHandler upgradeDecompress(ChannelHandler h) {
    return h.getClass() == CompressionDecoder.class
        ? new TrsCompressionDecoder(((CompressionDecoderAccess) h).tfl$threshold())
        : null;
  }

  public ChannelHandler upgradeCompress(ChannelHandler h) {
    return h.getClass() == CompressionEncoder.class
        ? new TrsCompressionEncoder(((CompressionEncoderAccess) h).tfl$threshold())
        : null;
  }

  public boolean vanillaDecrypt(ChannelHandler h) {
    return h.getClass() == net.minecraft.network.encryption.PacketDecryptor.class;
  }

  public long pongTime(Object p) {
    return NONE;
  }

  public long gameTime(Object p) {
    return p instanceof net.minecraft.network.packet.s2c.play.WorldTimeS2CPacket time
        ? time.getTime()
        : NONE;
  }

  public long keepAliveId(Object p) {
    return p instanceof net.minecraft.network.packet.s2c.play.KeepAliveS2CPacket keep
        ? keep.getTimeMillis()
        : NONE;
  }

  public boolean activePing() {
    return false;
  }

  public boolean sendPing(long time) {
    return false;
  }

  public long millis() {
    return System.nanoTime() / 1000000;
  }

  public int serverLatency() {
    if (c.mc.player == null || c.mc.getNetworkHandler() == null) return -1;
    var info = c.mc.getNetworkHandler().getOnlinePlayer(c.mc.player.getUuid());
    return info == null ? -1 : info.getPing();
  }

  public boolean multiplayer() {
    return c.mc.world != null && c.mc.player != null && !c.mc.isSingleplayer();
  }

  public void tick() {
    var handler = c.mc.getNetworkHandler();
    var channel =
        handler == null ? null : ((ConnectionAccess) handler.getConnection()).tfl$channel();
    NetBoost.tick(
        channel,
        c.capabilities.active(c.modules.ping),
        Math.round(c.modules.pingInterval.get() * 1000),
        Math.round(c.modules.pingSpikeThreshold.get()));
  }
}
