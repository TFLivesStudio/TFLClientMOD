package dev.theredstonee.trsclient.ornithe.net;

import dev.theredstonee.trsclient.core.net.NetCodecs;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.DecoderException;
import java.util.List;
import net.minecraft.network.CompressionDecoder;

/**
 * Vanillas Entpacker (Forge 1.8.9–1.12.2) ohne die zwei {@code new byte[]} je Paket: gleiche
 * Prüfungen und Meldungen, gleiches Ergebnis (fehlende Bytes bleiben 0 wie bei Vanilla).
 * Unterklasse, damit Vanillas {@code instanceof} greift.
 */
public final class TrsCompressionDecoder extends CompressionDecoder {
  private final NetCodecs.Inflate inflate = new NetCodecs.Inflate();
  private int threshold;

  public TrsCompressionDecoder(int threshold) {
    super(threshold);
    this.threshold = threshold;
  }

  @Override
  public void setThreshold(int threshold) {
    super.setThreshold(threshold);
    this.threshold = threshold;
  }

  @Override
  protected void decode(ChannelHandlerContext ctx, ByteBuf in, List<Object> out) throws Exception {
    if (in.readableBytes() == 0) return;
    int size = NetCodecs.readVarInt(in);
    if (size == 0) {
      out.add(in.readBytes(in.readableBytes()));
      return;
    }
    if (size < threshold) {
      throw new DecoderException(
          "Badly compressed packet - size of "
              + size
              + " is below server threshold of "
              + threshold);
    }
    if (size > 2097152) {
      throw new DecoderException(
          "Badly compressed packet - size of "
              + size
              + " is larger than protocol maximum of "
              + 2097152);
    }
    out.add(inflate.inflate(ctx.alloc(), in, size, false));
  }

  @Override
  protected void handlerRemoved0(ChannelHandlerContext ctx) throws Exception {
    inflate.end();
    super.handlerRemoved0(ctx);
  }
}
