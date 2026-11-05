package dev.theredstonee.trsclient.core.hosting.e4mc;

import dev.theredstonee.trsclient.core.hosting.net.PeerStream;
import java.io.*;
import java.net.*;
import java.nio.file.Paths;
import java.util.concurrent.*;
import java.util.concurrent.atomic.*;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class WorldShareServiceTest {
    static final String BROKER="https://relay.test/allocate";
    static WorldShareService.Consent consent() { WorldShareService.Consent c=new WorldShareService.Consent(); c.understood(true); return c; }
    static class Tunnel implements PublicTunnel {
        final CountDownLatch ready=new CountDownLatch(1), stopped=new CountDownLatch(1);
        volatile Events events;
        public void start(String broker, Events events) { this.events=events; events.domain("guest.share.test"); ready.countDown(); }
        public void stop() { stopped.countDown(); }
    }
    static class Peer implements PeerStream {
        Sink sink; volatile boolean open=true;
        final BlockingQueue<byte[]> output=new LinkedBlockingQueue<>();
        final CountDownLatch closed=new CountDownLatch(1);
        public void start(Sink sink) { this.sink=sink; }
        public boolean write(byte[] b,int off,int len) { output.offer(java.util.Arrays.copyOfRange(b,off,off+len)); return open; }
        public void close(String reason) { if (open) { open=false; if(sink!=null)sink.closed(reason); closed.countDown(); } }
        public boolean isOpen() { return open; }
        public Path path() { return Path.PUBLIC; }
        public InetSocketAddress remoteAddress() { return new InetSocketAddress("127.0.0.1",0); }
        void send(byte[] bytes) { sink.data(bytes,0,bytes.length); }
    }
    static byte[] status() { return new byte[] {7,0,47,1,'x',0,1,1}; }
    static byte[] login(String name) throws IOException {
        ByteArrayOutputStream out=new ByteArrayOutputStream(); out.write(new byte[] {7,0,47,1,'x',0,1,2});
        out.write(name.length()+2); out.write(0); out.write(name.length()); out.write(name.getBytes("UTF-8")); return out.toByteArray();
    }
    @Test void consentAndHttpsAreRequiredBeforeAnyRuntimeWork() throws Exception {
        AtomicInteger calls=new AtomicInteger();
        WorldShareService s=new WorldShareService(Paths.get("unused"),(p,progress)->{calls.incrementAndGet(); return new Tunnel();});
        assertFalse(s.start(new Object(),12345,"Host",BROKER,new WorldShareService.Consent()));
        assertFalse(s.start(new Object(),12345,"Host","http://relay.test",consent()));
        assertFalse(s.start(new Object(),12345,"Host","https://secret@relay.test",consent()));
        assertEquals(0,calls.get()); assertEquals(WorldShareService.State.OFF,s.snapshot().state);
    }
    @Test void guestBytesReachActualLanAndStopPreservesLan() throws Exception {
        Tunnel tunnel=new Tunnel(); Object world=new Object();
        WorldShareService s=new WorldShareService(Paths.get("unused"),(p,progress)->tunnel);
        try(ServerSocket lan=new ServerSocket(0,4,InetAddress.getByName("127.0.0.1"))) {
            lan.setSoTimeout(3000);
            WorldShareService.Consent permission=consent();
            assertTrue(s.start(world,lan.getLocalPort(),"Host",BROKER,permission));
            assertTrue(tunnel.ready.await(3,TimeUnit.SECONDS));
            assertEquals("guest.share.test",s.snapshot().address);
            Peer peer=new Peer(); tunnel.events.stream(peer);
            byte[] handshake=status(); peer.send(handshake);
            try(Socket guest=lan.accept()) {
                guest.setSoTimeout(3000);
                byte[] received=new byte[handshake.length]; new DataInputStream(guest.getInputStream()).readFully(received);
                assertArrayEquals(handshake,received);
                guest.getOutputStream().write(new byte[]{1,2,3});
                assertArrayEquals(new byte[]{1,2,3},peer.output.poll(3,TimeUnit.SECONDS));
                s.tick(world); assertEquals(WorldShareService.State.ONLINE,s.snapshot().state);
                s.tick(new Object());
                assertTrue(peer.closed.await(3,TimeUnit.SECONDS));
                assertEquals(-1,guest.getInputStream().read());
            }
            assertTrue(tunnel.stopped.await(3,TimeUnit.SECONDS));
            assertEquals(WorldShareService.State.OFF,s.snapshot().state);
            tunnel.events.domain("stale.share.test");
            assertEquals("",s.snapshot().address);
            assertFalse(s.start(world,lan.getLocalPort(),"Host",BROKER,permission));
            // The service owns no LAN listener: local/direct play remains available after relay shutdown.
            try(Socket local=new Socket("127.0.0.1",lan.getLocalPort()); Socket accepted=lan.accept()) { assertTrue(accepted.isConnected()); }
        } finally { s.stop(); }
    }
    @Test void spoofedHostIsRejectedBeforeOpeningLanSocket() throws Exception {
        Tunnel tunnel=new Tunnel(); WorldShareService s=new WorldShareService(Paths.get("unused"),(p,progress)->tunnel);
        try(ServerSocket lan=new ServerSocket(0,4,InetAddress.getByName("127.0.0.1"))) {
            assertTrue(s.start(new Object(),lan.getLocalPort(),"Host",BROKER,consent()));
            assertTrue(tunnel.ready.await(3,TimeUnit.SECONDS));
            Peer peer=new Peer(); tunnel.events.stream(peer);
            byte[] data=login("hOsT");
            peer.send(java.util.Arrays.copyOfRange(data,0,5));
            peer.send(java.util.Arrays.copyOfRange(data,5,data.length));
            assertTrue(peer.closed.await(3,TimeUnit.SECONDS));
            lan.setSoTimeout(100); assertThrows(SocketTimeoutException.class,lan::accept);
        } finally { s.stop(); }
    }
    @Test void modernHandshakeBeginningWithLegacyPingByteCannotBypassHostGuard() throws Exception {
        // A 254-byte modern handshake starts FE 01, just like a legacy ping prefix.
        // Relays can route a legitimate name followed by NUL padding (Forge-style host data).
        ByteArrayOutputStream hello=new ByteArrayOutputStream();hello.write(0xfe);hello.write(1);
        hello.write(0);hello.write(47);hello.write(0xf7);hello.write(1);
        byte[] address=new byte[247];java.util.Arrays.fill(address,(byte)'x');address[20]=0;hello.write(address);
        hello.write(0);hello.write(1);hello.write(2);
        byte[] packet=login("hOsT");hello.write(packet,8,packet.length-8);
        dev.theredstonee.trsclient.core.hosting.LoginSniffer sniffer=new dev.theredstonee.trsclient.core.hosting.LoginSniffer(name->!name.equalsIgnoreCase("Host"));
        byte[] wire=hello.toByteArray();
        assertEquals(dev.theredstonee.trsclient.core.hosting.LoginSniffer.Verdict.REJECT,sniffer.feed(wire,0,wire.length));
    }
    @Test void relayFailureAndInboundFloodRevokeConnectionsWithoutClosingLan() throws Exception {
        Tunnel tunnel=new Tunnel(); WorldShareService s=new WorldShareService(Paths.get("unused"),(p,progress)->tunnel);
        try(ServerSocket lan=new ServerSocket(0,4,InetAddress.getByName("127.0.0.1"))) {
            assertTrue(s.start(new Object(),lan.getLocalPort(),"Host",BROKER,consent()));
            assertTrue(tunnel.ready.await(3,TimeUnit.SECONDS));
            Peer peer=new Peer(); tunnel.events.stream(peer); peer.send(new byte[WorldShareService.MAX_BYTES+1]);
            assertTrue(peer.closed.await(3,TimeUnit.SECONDS));
            tunnel.events.failed("private details must not reach UI");
            assertEquals(WorldShareService.State.ERROR,s.snapshot().state);
            assertEquals("connection",s.snapshot().error);
            assertTrue(tunnel.stopped.await(3,TimeUnit.SECONDS)); assertFalse(lan.isClosed());
        } finally { s.stop(); }
    }
    @Test void cancellationDuringPreparationCannotResurrectSession() throws Exception {
        Tunnel tunnel=new Tunnel(); CountDownLatch entered=new CountDownLatch(1), finish=new CountDownLatch(1);
        WorldShareService s=new WorldShareService(Paths.get("unused"),(p,progress)->{entered.countDown(); assertTrue(finish.await(3,TimeUnit.SECONDS)); return tunnel;});
        assertTrue(s.start(new Object(),12345,"Host",BROKER,consent()));
        assertTrue(entered.await(3,TimeUnit.SECONDS)); s.stop(); finish.countDown();
        assertTrue(tunnel.stopped.await(3,TimeUnit.SECONDS));
        assertNull(tunnel.events); assertEquals(WorldShareService.State.OFF,s.snapshot().state);
    }
}
