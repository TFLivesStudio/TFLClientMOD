package dev.theredstonee.trsclient.e4mcbridge;
import dev.theredstonee.trsclient.core.hosting.net.PeerStream;
import io.netty.buffer.*;
import io.netty.channel.*;
import io.netty.handler.codec.quic.QuicStreamChannel;
import io.netty.util.concurrent.ImmediateEventExecutor;
import org.junit.jupiter.api.Test;
import java.lang.reflect.Proxy;
import java.nio.charset.StandardCharsets;
import java.nio.channels.ClosedChannelException;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
class PeerBufferTest {
    static final class Transport {
        boolean active = true;
        final List<ByteBuf> bytes = new ArrayList<>();
        final List<ChannelPromise> writes = new ArrayList<>();
        final QuicStreamChannel channel;
        final QuiclimeTunnel.QuicPeerStream stream;
        Transport() {
            channel = (QuicStreamChannel)Proxy.newProxyInstance(getClass().getClassLoader(), new Class<?>[]{QuicStreamChannel.class}, (proxy, method, args) -> {
                if (method.getName().equals("isActive")) return active;
                if (method.getName().equals("writeAndFlush")) {
                    bytes.add((ByteBuf)args[0]);ChannelPromise promise = new DefaultChannelPromise((Channel)proxy, ImmediateEventExecutor.INSTANCE);
                    writes.add(promise);return promise;
                }
                if (method.getName().equals("close")) {
                    if (active) {
                        active = false;
                        for (int i=0;i<writes.size();i++) { bytes.get(i).release();writes.get(i).tryFailure(new ClosedChannelException()); }
                        streamInactive();
                    }
                    return new DefaultChannelPromise((Channel)proxy, ImmediateEventExecutor.INSTANCE).setSuccess();
                }
                if (method.getName().equals("toString")) return "test transport";
                throw new UnsupportedOperationException(method.getName());
            });
            stream = new QuiclimeTunnel.QuicPeerStream(channel);
        }
        void streamInactive() throws Exception { stream.handler().channelInactive(null); }
        void receive(byte[] value) throws Exception { stream.handler().channelRead(null, Unpooled.wrappedBuffer(value)); }
    }
    @Test void stalledSenderHasABoundedQueueAndClosesOnOverflow() {
        Transport t = new Transport();byte[] half = new byte[QuiclimeTunnel.QuicPeerStream.MAX_PENDING_BYTES/2];
        assertTrue(t.stream.write(half,0,half.length));assertTrue(t.stream.write(half,0,half.length));
        assertEquals(2L*half.length,t.stream.pendingBytes());
        assertFalse(t.stream.write(new byte[1],0,1));assertFalse(t.stream.isOpen());assertEquals(0,t.stream.pendingBytes());
        assertFalse(t.stream.write(half,0,half.length));
    }
    @Test void earlyTrafficCannotGrowWithoutASubscriber() throws Exception {
        Transport t = new Transport();t.receive(new byte[700000]);t.receive(new byte[500000]);assertFalse(t.stream.isOpen());
        List<String> data = new ArrayList<>();int[] closed={0};
        t.stream.start(new PeerStream.Sink(){public void data(byte[] b,int off,int len){data.add("unexpected");}public void closed(String reason){closed[0]++;}});
        assertTrue(data.isEmpty());assertEquals(1,closed[0]);
    }
    @Test void bufferedAndLiveBytesStayOrderedAndFinishOnce() throws Exception {
        Transport t=new Transport();t.receive(new byte[]{65});t.receive(new byte[]{66});StringBuilder data=new StringBuilder();int[] closed={0};
        PeerStream.Sink sink=new PeerStream.Sink(){public void data(byte[] b,int off,int len){data.append(new String(b,off,len,StandardCharsets.UTF_8));}public void closed(String reason){closed[0]++;}};
        t.stream.start(sink);t.receive(new byte[]{67});t.stream.close("done");t.stream.close("again");
        assertEquals("ABC",data.toString());assertEquals(1,closed[0]);assertThrows(IllegalStateException.class,()->t.stream.start(sink));
    }
}
