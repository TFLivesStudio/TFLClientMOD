package dev.theredstonee.trsclient.e4mcbridge;

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.*;
import io.netty.channel.nio.NioIoHandler;
import io.netty.channel.socket.nio.NioDatagramChannel;
import io.netty.handler.codec.quic.*;
import io.netty.util.concurrent.Future;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import javax.net.ssl.TrustManagerFactory;
import java.io.InputStream;
import java.net.InetSocketAddress;
import java.nio.file.Path;
import java.nio.file.Files;
import java.security.KeyStore;
import java.security.cert.CertificateFactory;
import java.util.concurrent.TimeUnit;
import static org.junit.jupiter.api.Assertions.*;

class RelayTlsTest {
    @TempDir Path directory;

    @Test void brokerRejectsPlaintextBeforeConnecting() {
        assertThrows(IllegalArgumentException.class, () -> QuiclimeTunnel.relay("http://127.0.0.1:1/broker"));
        assertThrows(IllegalArgumentException.class, () -> QuiclimeTunnel.relay("https://admin:secret@localhost/broker"));
    }
    @Test void trustedMatchingRelayCompletesHandshake() throws Exception { handshake("localhost", true, true); }
    @Test void trustedWrongNameIsRejected() throws Exception { handshake("wrong.invalid", true, false); }
    @Test void untrustedRelayIsRejected() throws Exception { handshake("localhost", false, false); }

    private void handshake(String hostname, boolean trustCertificate, boolean expected) throws Exception {
        Path key = directory.resolve("private.pem"), cert = directory.resolve("certificate.pem");
        Process openssl = new ProcessBuilder("openssl", "req", "-x509", "-newkey", "rsa:2048", "-nodes", "-days", "1",
            "-keyout", key.toString(), "-out", cert.toString(), "-subj", "/CN=localhost",
            "-addext", "subjectAltName=DNS:localhost").redirectErrorStream(true).redirectOutput(directory.resolve("openssl.log").toFile()).start();
        assertTrue(openssl.waitFor(20, TimeUnit.SECONDS));assertEquals(0, openssl.exitValue());
        TrustManagerFactory trust = null;
        if (trustCertificate) {
            KeyStore roots = KeyStore.getInstance(KeyStore.getDefaultType());roots.load(null, null);
            try (InputStream in = Files.newInputStream(cert)) {
                roots.setCertificateEntry("local-relay", CertificateFactory.getInstance("X.509").generateCertificate(in));
            }
            trust = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());trust.init(roots);
        }
        QuicSslContext serverTls = QuicSslContextBuilder.forServer(key.toFile(), null, cert.toFile()).applicationProtocols("quiclime").build();
        QuicSslContext clientTls = QuiclimeTunnel.tlsContext(trust);
        EventLoopGroup group = new MultiThreadIoEventLoopGroup(1, NioIoHandler.newFactory());
        Channel server = null, client = null;QuicChannel connection = null;
        try {
            ChannelHandler codec = new QuicServerCodecBuilder().sslContext(serverTls)
                .maxIdleTimeout(5, TimeUnit.SECONDS).initialMaxData(65536)
                .initialMaxStreamDataBidirectionalLocal(65536).initialMaxStreamDataBidirectionalRemote(65536)
                .initialMaxStreamsBidirectional(1).streamHandler(new ChannelInboundHandlerAdapter()).build();
            server = new Bootstrap().group(group).channel(NioDatagramChannel.class).handler(codec)
                .bind(new InetSocketAddress("127.0.0.1", 0)).sync().channel();
            final int port = ((InetSocketAddress)server.localAddress()).getPort();
            client = new Bootstrap().group(group).channel(NioDatagramChannel.class)
                .handler(new QuicClientCodecBuilder().sslContext(clientTls)
                    .sslEngineProvider(channel -> clientTls.newEngine(channel.alloc(), hostname, port))
                    .initialMaxStreamsBidirectional(1).maxIdleTimeout(5, TimeUnit.SECONDS).build())
                .bind(new InetSocketAddress("127.0.0.1", 0)).sync().channel();
            Future<QuicChannel> future = QuicChannel.newBootstrap(client).handler(new ChannelInboundHandlerAdapter())
                .remoteAddress(server.localAddress()).connect();
            assertTrue(future.await(10, TimeUnit.SECONDS), "Handshake did not finish");
            if (future.isSuccess()) connection = future.getNow();
            assertEquals(expected, future.isSuccess(), future.cause() == null ? "Unexpected accepted TLS peer" : future.cause().toString());
        } finally {
            if (connection != null) connection.close().syncUninterruptibly();
            if (client != null) client.close().syncUninterruptibly();
            if (server != null) server.close().syncUninterruptibly();
            group.shutdownGracefully(0, 2, TimeUnit.SECONDS).syncUninterruptibly();
            io.netty.util.ReferenceCountUtil.release(clientTls);
            io.netty.util.ReferenceCountUtil.release(serverTls);
        }
    }
}
