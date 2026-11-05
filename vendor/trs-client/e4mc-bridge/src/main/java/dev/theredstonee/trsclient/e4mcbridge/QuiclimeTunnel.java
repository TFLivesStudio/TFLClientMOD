/*
 * Öffentlicher Link über das e4mc-Relay ("QUIClime"-Protokoll).
 *
 * Angepasst aus e4mc_minecraft (QuiclimeSession.java, https://github.com/vgskye/e4mc-minecraft-architectury):
 * Copyright (c) 2024 Skye – MIT License (Volltext in META-INF/LICENSE-e4mc dieser JAR). Änderungen für den
 * TRS Client: Java 8, eigener ClassLoader ohne Minecraft-Abhängigkeiten, Übergabe jedes eingehenden Streams als
 * PeerStream an das TRS-Welt-Hosting, kein Dialtone/Iroh, keine Chat-Ausgaben.
 */
package dev.theredstonee.trsclient.e4mcbridge;

import dev.theredstonee.trsclient.core.hosting.e4mc.PublicTunnel;
import dev.theredstonee.trsclient.core.hosting.net.PeerStream;
import io.netty.bootstrap.Bootstrap;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.MultiThreadIoEventLoopGroup;
import io.netty.channel.nio.NioIoHandler;
import io.netty.channel.socket.nio.NioDatagramChannel;
import io.netty.handler.codec.ByteToMessageDecoder;
import io.netty.handler.codec.quic.QuicChannel;
import io.netty.handler.codec.quic.QuicClientCodecBuilder;
import io.netty.handler.codec.quic.QuicSslContext;
import io.netty.handler.codec.quic.QuicSslContextBuilder;
import io.netty.handler.codec.quic.QuicStreamChannel;
import io.netty.handler.codec.quic.QuicStreamType;
import io.netty.util.concurrent.DefaultThreadFactory;
import io.netty.util.concurrent.Future;
import io.netty.util.concurrent.GenericFutureListener;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Umsetzung von {@link PublicTunnel} – wird nur im isolierten ClassLoader geladen. */
public final class QuiclimeTunnel implements PublicTunnel {
	private static final Pattern KIND = Pattern.compile("\"kind\"\\s*:\\s*\"([a-z_]{1,40})\"");
	private static final Pattern DOMAIN = Pattern.compile("\"domain\"\\s*:\\s*\"([A-Za-z0-9.-]{1,253})\"");
	private static final Pattern MESSAGE = Pattern.compile("\"message\"\\s*:\\s*\"((?:[^\"\\\\]|\\\\.){0,400})\"");

	private volatile EventLoopGroup group;
	private volatile Channel datagram;
	private volatile QuicChannel quic;
	private volatile boolean stopped;
    private boolean started;
	private final javax.net.ssl.TrustManagerFactory trustedRoots;

	public QuiclimeTunnel() {
		this(null);
	}
	// Local acceptance tests supply their private test CA; production always uses system trust.
	QuiclimeTunnel(javax.net.ssl.TrustManagerFactory trustedRoots) { this.trustedRoots=trustedRoots; }

	/** Relay vom Broker: {"id":"de","host":"de.e4mc.link","port":25575}. */
	static String[] relay(String brokerUrl) throws Exception {
        URL endpoint = new URL(brokerUrl);
        if (!"https".equalsIgnoreCase(endpoint.getProtocol()) || endpoint.getUserInfo() != null)
            throw new IllegalArgumentException("Sharing broker requires HTTPS without embedded credentials");
		HttpURLConnection c = (HttpURLConnection) endpoint.openConnection();
		c.setConnectTimeout(10_000);
		c.setReadTimeout(10_000);
		c.setInstanceFollowRedirects(false);
		c.setRequestProperty("Accept", "application/json");
		c.setRequestProperty("User-Agent", "TRS-Client");
		try {
			if (c.getResponseCode() != 200) throw new IllegalStateException("broker HTTP " + c.getResponseCode());
			ByteArrayOutputStream out = new ByteArrayOutputStream();
			try (InputStream in = c.getInputStream()) {
				byte[] buf = new byte[1024];
				int n;
				while ((n = in.read(buf)) > 0 && out.size() < 8192) out.write(buf, 0, n);
			}
			String json = new String(out.toByteArray(), StandardCharsets.UTF_8);
			Matcher h = Pattern.compile("\"host\"\\s*:\\s*\"([A-Za-z0-9.-]{1,253})\"").matcher(json);
			Matcher p = Pattern.compile("\"port\"\\s*:\\s*([0-9]{1,5})").matcher(json);
			if (!h.find() || !p.find()) throw new IllegalStateException("broker answer");
			return new String[] { h.group(1), p.group(1) };
		} finally {
			c.disconnect();
		}
	}

    // Explicit identity verification, independent of Netty's configurable default.
    static QuicSslContext tlsContext(javax.net.ssl.TrustManagerFactory trust) {
        return QuicSslContextBuilder.forClient().trustManager(trust)
            .endpointIdentificationAlgorithm("HTTPS").applicationProtocols("quiclime").build();
    }

	@Override
	public void start(String brokerUrl, final Events events) throws Exception {
        synchronized (this) {
            if (stopped || started) throw new IllegalStateException("Tunnel instances are single-session");
            started = true;
        }
		String[] r = relay(brokerUrl);
        if (stopped) return;
		final String host = r[0];
		final int port = Integer.parseInt(r[1]);
		final QuicSslContext context = tlsContext(trustedRoots);
		io.netty.channel.ChannelHandler codec = new QuicClientCodecBuilder()
				.sslContext(context)
				.sslEngineProvider(ch -> context.newEngine(ch.alloc(), host, port))
				.initialMaxStreamsBidirectional(512)
				.maxIdleTimeout(10, TimeUnit.SECONDS)
				.initialMaxData(4611686018427387903L)
				.initialMaxStreamDataBidirectionalRemote(1250000)
				.initialMaxStreamDataBidirectionalLocal(1250000)
				.initialMaxStreamDataUnidirectional(1250000)
				.build();
        synchronized (this) {
            if (stopped) return;
            group = new MultiThreadIoEventLoopGroup(1, new DefaultThreadFactory("TFL-WorldShare-QUIC", true), NioIoHandler.newFactory());
        }
		final InetSocketAddress remote = new InetSocketAddress(InetAddress.getByName(host), port);
		new Bootstrap().group(group).channel(NioDatagramChannel.class).handler(codec).bind(0)
				.addListener((ChannelFutureListener) bound -> {
					if (!bound.isSuccess()) {
						if (!stopped) events.failed("bind: " + bound.cause());
						return;
					}
                    if (stopped) { bound.channel().close(); return; }
					datagram = bound.channel();
					QuicChannel.newBootstrap(datagram)
							.streamHandler(new ChannelInitializer<QuicStreamChannel>() {
								@Override
								protected void initChannel(QuicStreamChannel ch) {
									// Jeder vom Relay geöffnete Stream = ein Spieler (voller Minecraft-Bytestrom).
                                    if (stopped) { ch.close(); return; }
									QuicPeerStream s = new QuicPeerStream(ch);
									ch.pipeline().addLast(s.handler());
									events.stream(s);
								}
							})
							.handler(new ChannelInboundHandlerAdapter() {
								@Override
								public void channelInactive(ChannelHandlerContext ctx) throws Exception {
									super.channelInactive(ctx);
									if (!stopped) events.failed("connection closed");
								}

								@Override
								public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
									if (!stopped) events.failed(String.valueOf(cause));
								}
							})
							.remoteAddress(remote)
							.connect()
							.addListener((GenericFutureListener<Future<QuicChannel>>) connected -> {
								if (!connected.isSuccess()) {
									if (!stopped) events.failed("connect: " + connected.cause());
									return;
								}
                                if (stopped) { connected.getNow().close(); return; }
								quic = connected.getNow();
								openControl(quic, events);
							});
				});
	}

	private void openControl(QuicChannel q, final Events events) {
		q.createStream(QuicStreamType.BIDIRECTIONAL, new ChannelInitializer<QuicStreamChannel>() {
			@Override
			protected void initChannel(QuicStreamChannel ch) {
				ch.pipeline().addLast(new ControlDecoder(), new ChannelInboundHandlerAdapter() {
					@Override
					public void channelRead(ChannelHandlerContext ctx, Object msg) {
                        if (stopped) return;
						String json = (String) msg;
						Matcher k = KIND.matcher(json);
						if (!k.find()) return;
						String kind = k.group(1);
						if (kind.equals("domain_assignment_complete")) {
							Matcher d = DOMAIN.matcher(json);
							if (d.find()) events.domain(d.group(1));
						} else if (kind.equals("request_message_broadcast")) {
							Matcher m = MESSAGE.matcher(json);
							if (m.find()) events.broadcast(m.group(1).replace("\\\"", "\"").replace("\\\\", "\\"));
						}
					}
				});
			}
		}).addListener((GenericFutureListener<Future<QuicStreamChannel>>) f -> {
			if (!f.isSuccess()) {
				if (!stopped) events.failed("control: " + f.cause());
				return;
			}
            if (stopped) { f.getNow().close(); return; }
			QuicStreamChannel control = f.getNow();
			control.writeAndFlush(controlMessage("{\"kind\":\"probe_capabilities\"}"));
			control.writeAndFlush(controlMessage("{\"kind\":\"request_domain_assignment\"}"));
            // Keep the authenticated QUIC connection alive even when no guest is playing.
            // Capability probes are part of QUIClime in both Retro and modern e4mc.
            final java.util.concurrent.ScheduledFuture<?> heartbeat = control.eventLoop().scheduleAtFixedRate(() -> {
                if (!stopped && control.isActive()) control.writeAndFlush(controlMessage("{\"kind\":\"probe_capabilities\"}"));
            }, 4, 4, TimeUnit.SECONDS);
            control.closeFuture().addListener(done -> heartbeat.cancel(false));
		});
	}

	/** VarInt-Länge + JSON (serverbound). */
	static ByteBuf controlMessage(String json) {
		byte[] b = json.getBytes(StandardCharsets.UTF_8);
		ByteBuf out = Unpooled.buffer(b.length + 5);
		int v = b.length;
		while ((v & 0xFFFFFF80) != 0) {
			out.writeByte(v & 0x7F | 0x80);
			v >>>= 7;
		}
		out.writeByte(v);
		out.writeBytes(b);
		return out;
	}

	/** Clientbound: ein Längen-Byte + JSON (so schreibt es das Relay). */
	static final class ControlDecoder extends ByteToMessageDecoder {
		@Override
		protected void decode(ChannelHandlerContext ctx, ByteBuf in, List<Object> out) {
			while (in.readableBytes() >= 1) {
				int size = in.getUnsignedByte(in.readerIndex());
				if (in.readableBytes() < size + 1) return;
				in.skipBytes(1);
				byte[] b = new byte[size];
				in.readBytes(b);
				out.add(new String(b, StandardCharsets.UTF_8));
			}
		}
	}

	@Override
	public void stop() {
        QuicChannel q; Channel d; EventLoopGroup g;
        synchronized (this) {
            if (stopped) return;
            stopped = true; q = quic; d = datagram; g = group;
        }
		try {
			if (q != null) q.close().awaitUninterruptibly(2000);
			if (d != null) d.close().awaitUninterruptibly(2000);
		} finally {
			if (g != null) g.shutdownGracefully(0, 1, TimeUnit.SECONDS);
		}
	}

	/** Ein QUIC-Stream als {@link PeerStream}. */
	static final class QuicPeerStream implements PeerStream {
		private static final InetSocketAddress PLACEHOLDER = placeholder();
		private final QuicStreamChannel ch;
        static final int MAX_PENDING_BYTES = 1024 * 1024;
		private final List<byte[]> early = new ArrayList<byte[]>();
        private int earlyBytes;
        private final java.util.concurrent.atomic.AtomicInteger pending = new java.util.concurrent.atomic.AtomicInteger();
        private volatile boolean closing;
		private volatile Sink sink;
		private volatile boolean closedSeen;
		private String closedReason;

		QuicPeerStream(QuicStreamChannel ch) {
			this.ch = ch;
		}

		private static InetSocketAddress placeholder() {
			try {
				return new InetSocketAddress(InetAddress.getByAddress("e4mc-link", new byte[] { 127, 0, 0, 1 }), 0);
			} catch (java.net.UnknownHostException e) {
				return new InetSocketAddress(InetAddress.getLoopbackAddress(), 0);
			}
		}

		ChannelInboundHandlerAdapter handler() {
			return new ChannelInboundHandlerAdapter() {
				@Override
				public void channelRead(ChannelHandlerContext ctx, Object msg) {
					ByteBuf b = (ByteBuf) msg;
					try {
						byte[] data = new byte[b.readableBytes()];
						b.readBytes(data);
						deliver(data);
					} finally {
						b.release();
					}
				}

				@Override
				public void channelInactive(ChannelHandlerContext ctx) {
					ended("closed");
				}

				@Override
				public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
					ctx.close();
				}
			};
		}

		private void deliver(byte[] data) {
            Sink target;
            synchronized (early) {
                if (closing || closedSeen) return;
                target = sink;
                if (target == null) {
                    if (data.length <= MAX_PENDING_BYTES - earlyBytes) {
                        early.add(data); earlyBytes += data.length; return;
                    }
                    early.clear(); earlyBytes = 0;
                }
            }
            if (target == null) { close("receive buffer limit"); return; }
            target.data(data, 0, data.length);
        }

		private void ended(String reason) {
			Sink s;
			synchronized (early) {
				if (closedSeen) return;
				closedSeen = true;
				if (closedReason == null) closedReason = reason;
				s = sink;
			}
			if (s != null) s.closed(reason);
		}

		@Override
		public void start(Sink s) {
			boolean closed;
			synchronized (early) {
                if (sink != null) throw new IllegalStateException("Peer stream already started");
				for (byte[] b : early) s.data(b, 0, b.length);
				early.clear(); earlyBytes = 0;
				sink = s;
				closed = closedSeen;
			}
			if (closed) s.closed(closedReason == null ? "closed" : closedReason);
		}

		@Override
		public boolean write(byte[] b, int off, final int len) {
            if (b == null) throw new NullPointerException("bytes");
            if (off < 0 || len < 0 || off > b.length - len) throw new IndexOutOfBoundsException();
            if (!isOpen()) return false;
            int current;
            do {
                current = pending.get();
                if (len > MAX_PENDING_BYTES - current) { close("send buffer limit"); return false; }
            } while (!pending.compareAndSet(current, current + len));
            ByteBuf copy = Unpooled.copiedBuffer(b, off, len);
            try {
                ch.writeAndFlush(copy).addListener((ChannelFutureListener) result -> {
                    pending.addAndGet(-len);
                    if (!result.isSuccess()) close("send failed");
                });
                return true;
            } catch (RuntimeException failure) {
                copy.release(); pending.addAndGet(-len); close("send failed"); return false;
            }
        }

        @Override public long pendingBytes() { return pending.get(); }

        @Override public void close(String reason) {
            synchronized (early) {
                if (closing) return;
                closing = true; closedReason = reason;
            }
            // Netty delivers channelInactive on the event loop, preserving callback ordering.
            ch.close();
        }

        @Override public boolean isOpen() { return !closing && !closedSeen && ch.isActive(); }

		@Override
		public Path path() {
			return Path.PUBLIC;
		}

		@Override
		public InetSocketAddress remoteAddress() {
			return PLACEHOLDER;
		}
	}
}
