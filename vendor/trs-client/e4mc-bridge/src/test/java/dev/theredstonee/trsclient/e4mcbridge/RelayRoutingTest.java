package dev.theredstonee.trsclient.e4mcbridge;

import dev.theredstonee.trsclient.e4mcbridge.relay.TflRelay;
import dev.theredstonee.trsclient.core.hosting.e4mc.PublicTunnel;
import dev.theredstonee.trsclient.core.hosting.net.PeerStream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import javax.net.ssl.*;
import java.io.*;
import java.net.*;
import java.nio.file.*;
import java.security.*;
import java.security.cert.CertificateFactory;
import java.util.concurrent.*;
import java.util.concurrent.atomic.*;
import static org.junit.jupiter.api.Assertions.*;

class RelayRoutingTest {
    @TempDir Path dir;
    @Test void actualHttpsBrokerQuicLeaseAndVanillaTcpRoutingAreRevokedTogether() throws Exception {
        Path key=dir.resolve("private.pem"),cert=dir.resolve("certificate.pem");
        Process openssl=new ProcessBuilder("openssl","req","-x509","-newkey","rsa:2048","-nodes","-days","1","-keyout",key.toString(),"-out",cert.toString(),"-subj","/CN=localhost","-addext","subjectAltName=DNS:localhost")
            .redirectErrorStream(true).redirectOutput(dir.resolve("openssl.log").toFile()).start();
        assertTrue(openssl.waitFor(20,TimeUnit.SECONDS));assertEquals(0,openssl.exitValue());
        KeyStore store=KeyStore.getInstance(KeyStore.getDefaultType());store.load(null,null);
        try(InputStream in=Files.newInputStream(cert)){store.setCertificateEntry("test-ca",CertificateFactory.getInstance("X.509").generateCertificate(in));}
        TrustManagerFactory trust=TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());trust.init(store);
        SSLContext verified=SSLContext.getInstance("TLS");verified.init(null,trust.getTrustManagers(),null);
        SSLSocketFactory old=HttpsURLConnection.getDefaultSSLSocketFactory();
        HttpsURLConnection.setDefaultSSLSocketFactory(verified.getSocketFactory());
        AtomicReference<String> address=new AtomicReference<>();CountDownLatch assigned=new CountDownLatch(1),ended=new CountDownLatch(1);
        AtomicReference<String> failure=new AtomicReference<>();BlockingQueue<byte[]> received=new LinkedBlockingQueue<>();
        AtomicReference<PeerStream> stream=new AtomicReference<>();
        QuiclimeTunnel tunnel=new QuiclimeTunnel(trust);
        try(TflRelay relay=new TflRelay("localhost","share.test",key,cert)) {
            relay.start("127.0.0.1",0,0,0);
            tunnel.start("https://localhost:"+relay.brokerPort()+"/getBestRelay",new PublicTunnel.Events(){
                public void domain(String domain){address.set(domain);assigned.countDown();}
                public void stream(PeerStream peer){stream.set(peer);peer.start(new PeerStream.Sink(){
                    public void data(byte[] bytes,int off,int len){received.offer(java.util.Arrays.copyOfRange(bytes,off,off+len));}
                    public void closed(String reason){ended.countDown();}
                });}
                public void broadcast(String message){}
                public void failed(String error){failure.set(error);assigned.countDown();}
            });
            assertTrue(assigned.await(10,TimeUnit.SECONDS));assertNull(failure.get(),failure.get());
            assertTrue(address.get().matches("[a-f0-9]{32}\\.share\\.test"));assertEquals(1,relay.activeSessions());
            try(Socket guest=new Socket("127.0.0.1",relay.guestPort())) {
                guest.setSoTimeout(5000);
                byte[] name=address.get().getBytes("UTF-8");ByteArrayOutputStream hello=new ByteArrayOutputStream();
                hello.write(name.length+6);hello.write(0);hello.write(47);hello.write(name.length);hello.write(name);hello.write(0x63);hello.write(0xdd);hello.write(1);
                byte[] bytes=hello.toByteArray();guest.getOutputStream().write(bytes,0,3);guest.getOutputStream().write(bytes,3,bytes.length-3);
                assertArrayEquals(bytes,received.poll(5,TimeUnit.SECONDS));assertNotNull(stream.get());
                assertTrue(stream.get().write(new byte[]{2,0,1},0,3));byte[] reply=new byte[3];new DataInputStream(guest.getInputStream()).readFully(reply);assertArrayEquals(new byte[]{2,0,1},reply);
                tunnel.stop();assertTrue(ended.await(5,TimeUnit.SECONDS));assertEquals(-1,guest.getInputStream().read());
            }
            long until=System.nanoTime()+TimeUnit.SECONDS.toNanos(5);
            while(relay.activeSessions()!=0 && System.nanoTime()<until)Thread.yield();
            assertEquals(0,relay.activeSessions());
            // A formerly allocated name no longer routes new vanilla connections.
            try(Socket revoked=new Socket("127.0.0.1",relay.guestPort())) {
                revoked.setSoTimeout(2000);byte[] host=address.get().getBytes("UTF-8");ByteArrayOutputStream hello=new ByteArrayOutputStream();
                hello.write(host.length+6);hello.write(0);hello.write(47);hello.write(host.length);hello.write(host);hello.write(0);hello.write(1);hello.write(1);
                revoked.getOutputStream().write(hello.toByteArray());assertEquals(-1,revoked.getInputStream().read());
            }
            // A single relay must serve subsequent hosts, not just its first QUIC connection.
            QuiclimeTunnel second=new QuiclimeTunnel(trust);
            CountDownLatch reassigned=new CountDownLatch(1);AtomicReference<String> secondDomain=new AtomicReference<>();
            AtomicReference<String> secondFailure=new AtomicReference<>();
            try {
                second.start("https://localhost:"+relay.brokerPort()+"/getBestRelay",new PublicTunnel.Events(){
                    public void domain(String domain){secondDomain.set(domain);reassigned.countDown();}
                    public void stream(PeerStream peer){peer.close("no guest in this session");}
                    public void broadcast(String message){}
                    public void failed(String error){secondFailure.set(error);reassigned.countDown();}
                });
                assertTrue(reassigned.await(8,TimeUnit.SECONDS),"A fresh host must obtain a new lease");
                assertNull(secondFailure.get(),secondFailure.get());assertNotNull(secondDomain.get());
                assertNotEquals(address.get(),secondDomain.get());assertEquals(1,relay.activeSessions());
            }finally{second.stop();}
        } finally {tunnel.stop();HttpsURLConnection.setDefaultSSLSocketFactory(old);}
    }
}
