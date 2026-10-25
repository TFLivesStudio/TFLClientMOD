package dev.theredstonee.trsclient.ornithe.net;

import dev.theredstonee.trsclient.core.net.NetCodecs;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import net.minecraft.network.CompressionEncoder;

/**
 * Vanillas Packer (Forge 1.8.9–1.12.2) ohne {@code new byte[]} je Paket – gleiche Schwelle, gleiche
 * zlib-Stufe, gleiche Bytes auf der Leitung. Unterklasse, damit Vanillas {@code instanceof} weiter
 * greift.
 */
public final class TrsCompressionEncoder extends CompressionEncoder {
  private final NetCodecs.Deflate deflate = new NetCodecs.Deflate();
  private int threshold;

  public TrsCompressionEncoder(int threshold) {
    super(threshold);
    this.threshold = threshold;
  }

  @Override
  public void setThreshold(int threshold) {
    super.setThreshold(threshold);
    this.threshold = threshold;
  }

  @Override
  protected void encode(ChannelHandlerContext ctx, ByteBuf in, ByteBuf out) throws Exception {
    int size = in.readableBytes();
    if (size < threshold) {
      NetCodecs.writeVarInt(out, 0);
      out.writeBytes(in);
    } else {
      NetCodecs.writeVarInt(out, size);
      deflate.deflate(in, out);
    }
  }

  @Override
  public void handlerRemoved(ChannelHandlerContext ctx) throws Exception {
    deflate.end();
    super.handlerRemoved(ctx);
  }
}
