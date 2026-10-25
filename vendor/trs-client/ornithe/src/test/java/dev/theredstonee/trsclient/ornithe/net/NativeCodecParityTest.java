package dev.theredstonee.trsclient.ornithe.net;

import dev.theredstonee.trsclient.core.net.NetCodecs;
import io.netty.buffer.*;
import io.netty.channel.embedded.EmbeddedChannel;
import io.netty.handler.codec.DecoderException;
import net.minecraft.network.*;
import org.junit.jupiter.api.Test;
import java.util.Random;
import static org.junit.jupiter.api.Assertions.*;

class NativeCodecParityTest {
 private static void release(EmbeddedChannel channel){channel.finish();Object message;while((message=channel.readInbound())!=null)io.netty.util.ReferenceCountUtil.release(message);while((message=channel.readOutbound())!=null)io.netty.util.ReferenceCountUtil.release(message);}
 private static byte[] take(EmbeddedChannel channel){ByteBuf buffer=(ByteBuf)channel.readOutbound();assertNotNull(buffer);try{byte[] bytes=new byte[buffer.readableBytes()];buffer.readBytes(bytes);return bytes;}finally{buffer.release();}}
 @Test void wireBytesAndDecodedPayloadMatchNativeAcrossThresholds(){
  var ours=new TrsCompressionEncoder(256);var vanilla=new CompressionEncoder(256);
  EmbeddedChannel encoded=new EmbeddedChannel(ours),nativeEncoded=new EmbeddedChannel(vanilla),decoded=new EmbeddedChannel(new TrsCompressionDecoder(256)),nativeDecoded=new EmbeddedChannel(new CompressionDecoder(256));
  try{for(int size:new int[]{1,127,255,256,257,4096,32768}){byte[] input=new byte[size];new Random(size).nextBytes(input);assertTrue(encoded.writeOutbound(Unpooled.wrappedBuffer(input.clone())));assertTrue(nativeEncoded.writeOutbound(Unpooled.wrappedBuffer(input.clone())));byte[] frame=take(encoded);assertArrayEquals(take(nativeEncoded),frame,"wire bytes at "+size);assertTrue(decoded.writeInbound(Unpooled.wrappedBuffer(frame.clone())));assertTrue(nativeDecoded.writeInbound(Unpooled.wrappedBuffer(frame)));ByteBuf actual=(ByteBuf)decoded.readInbound(),expected=(ByteBuf)nativeDecoded.readInbound();try{byte[] a=new byte[actual.readableBytes()],b=new byte[expected.readableBytes()];actual.readBytes(a);expected.readBytes(b);assertArrayEquals(input,a);assertArrayEquals(b,a);}finally{actual.release();expected.release();}}}finally{release(encoded);release(nativeEncoded);release(decoded);release(nativeDecoded);}
 }
 @Test void nativeAndSharedDecodersRejectInvalidDeclaredLengths(){for(int declared:new int[]{2,2097153}){for(var decoder:new io.netty.channel.ChannelHandler[]{new CompressionDecoder(256),new TrsCompressionDecoder(256)}){var channel=new EmbeddedChannel(decoder);ByteBuf frame=Unpooled.buffer();NetCodecs.writeVarInt(frame,declared);frame.writeByte(0);try{assertThrows(DecoderException.class,()->channel.writeInbound(frame));}finally{release(channel);}}}}
}
