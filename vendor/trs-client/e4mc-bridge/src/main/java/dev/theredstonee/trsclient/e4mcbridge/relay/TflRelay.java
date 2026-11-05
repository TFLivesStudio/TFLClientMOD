package dev.theredstonee.trsclient.e4mcbridge.relay;

import com.sun.net.httpserver.*;
import io.netty.bootstrap.*;
import io.netty.buffer.*;
import io.netty.channel.*;
import io.netty.channel.nio.NioIoHandler;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.*;
import io.netty.handler.codec.ByteToMessageDecoder;
import io.netty.handler.codec.quic.*;
import io.netty.handler.timeout.ReadTimeoutHandler;
import io.netty.util.*;
import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.security.*;
import java.security.cert.*;
import java.security.spec.PKCS8EncodedKeySpec;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.*;
import java.util.regex.*;
import javax.net.ssl.*;

/** Local/deployable HTTPS broker and QUIClime relay. No production configuration is supplied implicitly. */
public final class TflRelay implements AutoCloseable {
    private static final int MAX_SESSIONS=32, MAX_GUESTS=8, MAX_PENDING=1024*1024;
    private static final AttributeKey<Session> SESSION=AttributeKey.valueOf("tfl-share-session");
    private final EventLoopGroup group=new MultiThreadIoEventLoopGroup(2,NioIoHandler.newFactory());
    private final Map<String,Session> routes=new ConcurrentHashMap<String,Session>();
    private final Set<Session> sessions=Collections.newSetFromMap(new ConcurrentHashMap<Session,Boolean>());
    private final Map<InetAddress,AtomicInteger> rate=new ConcurrentHashMap<InetAddress,AtomicInteger>();
    private final Semaphore sessionSlots=new Semaphore(MAX_SESSIONS), guestSlots=new Semaphore(MAX_SESSIONS*MAX_GUESTS);
    private final SecureRandom random=new SecureRandom();
    private final String relayHost, suffix;
    private final QuicSslContext tls;
    private final SSLContext httpsTls;
    private final java.util.concurrent.ExecutorService brokerExecutor=new ThreadPoolExecutor(2,2,0,TimeUnit.SECONDS,
        new ArrayBlockingQueue<Runnable>(32),r->{Thread t=new Thread(r,"TFL-Broker");t.setDaemon(true);return t;});
    private Channel udp,tcp;
    private HttpsServer broker;
    private final AtomicBoolean closing=new AtomicBoolean();
    public TflRelay(String relayHost,String suffix,Path key,Path cert) throws Exception {
        if(!relayHost.matches("[A-Za-z0-9.-]{1,253}") || !suffix.matches("[a-z0-9.-]{3,180}") || !suffix.contains("."))throw new IllegalArgumentException("Invalid host/domain");
        this.relayHost=relayHost;this.suffix=suffix;
        tls=QuicSslContextBuilder.forServer(key.toFile(),null,cert.toFile()).applicationProtocols("quiclime").build();
        httpsTls=httpsContext(key,cert);
    }
    /** Explicit binds. Default launcher below binds only loopback; deployment requires operator configuration. */
    public void start(String bind,int httpsPort,int quicPort,int guestPort) throws Exception {
        try {
            ChannelHandler codec=new QuicServerCodecBuilder().sslContext(tls).tokenHandler(new RetryTokens())
                .maxIdleTimeout(15,TimeUnit.SECONDS).initialMaxData(8L*MAX_PENDING)
                .initialMaxStreamDataBidirectionalLocal(MAX_PENDING).initialMaxStreamDataBidirectionalRemote(MAX_PENDING)
                .initialMaxStreamsBidirectional(1)
                .handler(new ChannelInboundHandlerAdapter(){
                    public void channelActive(ChannelHandlerContext ctx) {
                        QuicChannel q=(QuicChannel)ctx.channel();
                        if(!sessionSlots.tryAcquire()) { q.close();return; }
                        Session s=new Session(q);sessions.add(s);q.attr(SESSION).set(s);
                        q.closeFuture().addListener(done->s.close());
                        q.eventLoop().schedule(()->{if(s.domain==null)s.close();},10,TimeUnit.SECONDS);
                        q.eventLoop().schedule(s::close,2,TimeUnit.HOURS);
                        ctx.fireChannelActive();
                    }
                }).streamHandler(new ChannelInitializer<QuicStreamChannel>() {
                    protected void initChannel(QuicStreamChannel ch) {
                        Session s=ch.parent().attr(SESSION).get();
                        if(s==null || !s.control.compareAndSet(false,true)){ch.close();return;}
                        ch.pipeline().addLast(new Control(s));
                        ch.closeFuture().addListener(done->s.close());
                    }
                }).build();
            udp=new Bootstrap().group(group).channel(NioDatagramChannel.class).handler(codec)
                .bind(new InetSocketAddress(bind,quicPort)).sync().channel();
            tcp=new ServerBootstrap().group(group).channel(NioServerSocketChannel.class)
                .childOption(ChannelOption.TCP_NODELAY,true)
                .childOption(ChannelOption.WRITE_BUFFER_WATER_MARK,new WriteBufferWaterMark(32768,65536))
                .childHandler(new ChannelInitializer<SocketChannel>() {
                    protected void initChannel(SocketChannel ch) {
                        if(!guestSlots.tryAcquire()) {ch.close();return;}
                        ch.closeFuture().addListener(done->guestSlots.release());
                        if(!allow(((InetSocketAddress)ch.remoteAddress()).getAddress())){ch.close();return;}
                        ch.pipeline().addLast(new ReadTimeoutHandler(30),new Route());
                        ch.eventLoop().schedule(()->{if(ch.pipeline().get(Route.class)!=null)ch.close();},5,TimeUnit.SECONDS);
                    }
                }).bind(new InetSocketAddress(bind,guestPort)).sync().channel();
            broker=HttpsServer.create(new InetSocketAddress(bind,httpsPort),32);
            broker.setHttpsConfigurator(new HttpsConfigurator(httpsTls));broker.setExecutor(brokerExecutor);
            broker.createContext("/getBestRelay",exchange->{
                try {
                    if(!"GET".equals(exchange.getRequestMethod()) || !"/getBestRelay".equals(exchange.getRequestURI().getPath())){exchange.sendResponseHeaders(404,-1);return;}
                    if(!allow(exchange.getRemoteAddress().getAddress())){exchange.sendResponseHeaders(429,-1);return;}
                    if(sessionSlots.availablePermits()==0){exchange.sendResponseHeaders(503,-1);return;}
                    byte[] json=("{\"id\":\"tfl\",\"host\":\""+relayHost+"\",\"port\":"+quicPort()+"}").getBytes(StandardCharsets.UTF_8);
                    exchange.getResponseHeaders().set("Content-Type","application/json");exchange.getResponseHeaders().set("Cache-Control","no-store");
                    exchange.sendResponseHeaders(200,json.length);exchange.getResponseBody().write(json);
                } finally { exchange.close(); }
            });broker.start();
            group.next().scheduleAtFixedRate(rate::clear,1,1,TimeUnit.MINUTES);
        } catch(Exception e) {close();throw e;}
    }
    private boolean allow(InetAddress ip) {
        AtomicInteger count=rate.get(ip);
        if(count==null){if(rate.size()>=1024)return false;AtomicInteger fresh=new AtomicInteger();AtomicInteger old=rate.putIfAbsent(ip,fresh);count=old==null?fresh:old;}
        return count.incrementAndGet()<=100;
    }
    public int quicPort(){return ((InetSocketAddress)udp.localAddress()).getPort();}
    public int guestPort(){return ((InetSocketAddress)tcp.localAddress()).getPort();}
    public int brokerPort(){return broker.getAddress().getPort();}
    public int activeSessions(){return routes.size();}
    private String domain() {byte[] id=new byte[16];random.nextBytes(id);StringBuilder name=new StringBuilder();for(byte b:id)name.append(String.format(Locale.ROOT,"%02x",b&255));return name+"."+suffix;}
    private final class Session {
        final QuicChannel channel;
        final AtomicBoolean control=new AtomicBoolean(),closed=new AtomicBoolean();
        final AtomicInteger guests=new AtomicInteger();
        final Set<Channel> sockets=Collections.newSetFromMap(new ConcurrentHashMap<Channel,Boolean>());
        volatile String domain;
        Session(QuicChannel channel){this.channel=channel;}
        synchronized String assign(){if(closed.get())return null;if(domain==null){domain=domain();routes.put(domain,this);}return domain;}
        synchronized void close(){if(!closed.compareAndSet(false,true))return;if(domain!=null)routes.remove(domain,this);sessions.remove(this);for(Channel socket:sockets)socket.close();channel.close();sessionSlots.release();}
    }
    private final class Control extends ByteToMessageDecoder {
        final Session session;
        final Pattern kind=Pattern.compile("\\\"kind\\\"\\s*:\\s*\\\"([a-z_]{1,40})\\\"");
        Control(Session session){this.session=session;}
        protected void decode(ChannelHandlerContext ctx,ByteBuf in,List<Object> out) {
            if(session.closed.get()){ctx.close();return;}
            while(in.isReadable()) {
                in.markReaderIndex();int size=0,shift=0;
                while(true){if(!in.isReadable()){in.resetReaderIndex();return;}int b=in.readUnsignedByte();size|=(b&127)<<shift;if((b&128)==0)break;shift+=7;if(shift>14){ctx.close();return;}}
                if(size<=0||size>4096){ctx.close();return;}if(in.readableBytes()<size){in.resetReaderIndex();return;}
                String json=in.readCharSequence(size,StandardCharsets.UTF_8).toString();Matcher m=kind.matcher(json);
                if(!m.find()){ctx.close();return;}
                if(m.group(1).equals("probe_capabilities"))reply(ctx,"{\"kind\":\"has_capabilities\",\"caps\":[]}");
                else if(m.group(1).equals("request_domain_assignment")){
                    String assigned=session.assign();if(assigned==null){ctx.close();return;}
                    reply(ctx,"{\"kind\":\"domain_assignment_complete\",\"domain\":\""+assigned+"\"}");
                } else {ctx.close();return;}
            }
        }
        void reply(ChannelHandlerContext ctx,String json){byte[] bytes=json.getBytes(StandardCharsets.UTF_8);if(bytes.length>255){ctx.close();return;}ctx.writeAndFlush(Unpooled.buffer(bytes.length+1).writeByte(bytes.length).writeBytes(bytes));}
        public void exceptionCaught(ChannelHandlerContext ctx,Throwable cause){ctx.close();}
    }
    /** Parses only the ordinary Minecraft handshake. It is retained and forwarded byte-for-byte. */
    private final class Route extends ByteToMessageDecoder {
        private boolean opening;
        protected void decode(ChannelHandlerContext ctx,ByteBuf in,List<Object> out) {
            if(opening)return;
            if(in.readableBytes()>4096){ctx.close();return;}
            ByteBuf view=in.duplicate();String host;
            try {
                int size=varint(view);if(size<0||size>2048){ctx.close();return;}if(view.readableBytes()<size)return;
                int end=view.readerIndex()+size;if(varint(view)!=0){ctx.close();return;}varint(view);
                int length=varint(view);if(length<1||length>1024||view.readerIndex()+length+3>end){ctx.close();return;}
                host=view.readCharSequence(length,StandardCharsets.UTF_8).toString().split("\\u0000",2)[0].toLowerCase(Locale.ROOT);
            } catch(IndexOutOfBoundsException incomplete){return;}
            Session session=routes.get(host);
            if(session==null||session.closed.get()||session.guests.incrementAndGet()>MAX_GUESTS){if(session!=null)session.guests.decrementAndGet();ctx.close();return;}
            Channel guest=ctx.channel();session.sockets.add(guest);
            opening=true;
            guest.closeFuture().addListener(done->{session.sockets.remove(guest);session.guests.decrementAndGet();});
            guest.config().setAutoRead(false);
            ByteBuf first=in.readRetainedSlice(in.readableBytes());
            session.channel.createStream(QuicStreamType.BIDIRECTIONAL,new ChannelInboundHandlerAdapter()).addListener(f->guest.eventLoop().execute(()->{
                if(!f.isSuccess()||!guest.isActive()||session.closed.get()){first.release();guest.close();if(f.isSuccess())((Channel)f.getNow()).close();return;}
                QuicStreamChannel stream=(QuicStreamChannel)f.getNow();
                stream.pipeline().addLast(new Pipe(guest));guest.pipeline().addLast(new Pipe(stream));
                stream.writeAndFlush(first).addListener(done->{if(!done.isSuccess()){stream.close();guest.close();}});
                guest.pipeline().remove(this);
                guest.config().setAutoRead(true);guest.read();
            }));
        }
        public void exceptionCaught(ChannelHandlerContext ctx,Throwable cause){ctx.close();}
    }
    private static int varint(ByteBuf input){int value=0;for(int i=0;i<5;i++){int b=input.readUnsignedByte();value|=(b&127)<<(i*7);if((b&128)==0)return value;}return -1;}
    private static final class Pipe extends ChannelInboundHandlerAdapter {
        final Channel other;final AtomicInteger pending=new AtomicInteger();
        Pipe(Channel other){this.other=other;}
        public void channelRead(ChannelHandlerContext ctx,Object msg) {
            if(!(msg instanceof ByteBuf)){ReferenceCountUtil.release(msg);ctx.close();return;}
            ByteBuf bytes=(ByteBuf)msg;int size=bytes.readableBytes();
            if(!other.isActive()||pending.addAndGet(size)>MAX_PENDING){bytes.release();ctx.close();other.close();return;}
            if(!other.isWritable())ctx.channel().config().setAutoRead(false);
            other.writeAndFlush(bytes).addListener(done->{pending.addAndGet(-size);if(!done.isSuccess()){ctx.close();other.close();}else if(other.isWritable())ctx.channel().config().setAutoRead(true);});
        }
        public void channelInactive(ChannelHandlerContext ctx){other.close();}
        public void channelWritabilityChanged(ChannelHandlerContext ctx){if(other.isActive())other.config().setAutoRead(ctx.channel().isWritable());ctx.fireChannelWritabilityChanged();}
        public void exceptionCaught(ChannelHandlerContext ctx,Throwable cause){ctx.close();other.close();}
    }
    private static SSLContext httpsContext(Path key,Path certificate) throws Exception {
        String pem=new String(Files.readAllBytes(key),StandardCharsets.US_ASCII).replace("-----BEGIN PRIVATE KEY-----","").replace("-----END PRIVATE KEY-----","").replaceAll("\\s","");
        PrivateKey privateKey=KeyFactory.getInstance("RSA").generatePrivate(new PKCS8EncodedKeySpec(Base64.getDecoder().decode(pem)));
        java.security.cert.Certificate[] chain;
        try(InputStream in=Files.newInputStream(certificate)){Collection<? extends java.security.cert.Certificate> certs=CertificateFactory.getInstance("X.509").generateCertificates(in);chain=certs.toArray(new java.security.cert.Certificate[0]);}
        KeyStore store=KeyStore.getInstance("PKCS12");store.load(null,null);char[] pass=new char[0];store.setKeyEntry("relay",privateKey,pass,chain);
        KeyManagerFactory keys=KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());keys.init(store,pass);
        SSLContext context=SSLContext.getInstance("TLS");context.init(keys.getKeyManagers(),null,null);return context;
    }
    public void close() {
        if(!closing.compareAndSet(false,true))return;
        if(broker!=null)broker.stop(0);for(Session session:sessions)session.close();
        if(tcp!=null)tcp.close().syncUninterruptibly();if(udp!=null)udp.close().syncUninterruptibly();
        brokerExecutor.shutdownNow();group.shutdownGracefully(0,1,TimeUnit.SECONDS).syncUninterruptibly();ReferenceCountUtil.safeRelease(tls);
    }
    public static void main(String[] args) throws Exception {
        Map<String,String> options=new HashMap<String,String>();for(String arg:args){String[] pair=arg.replaceFirst("^--","").split("=",2);if(pair.length!=2)throw new IllegalArgumentException("Use --key=value");options.put(pair[0],pair[1]);}
        final TflRelay relay=new TflRelay(options.getOrDefault("relay-host","localhost"),options.getOrDefault("suffix","share.test"),Paths.get(options.getOrDefault("key","private.pem")),Paths.get(options.getOrDefault("cert","certificate.pem")));
        relay.start(options.getOrDefault("bind","127.0.0.1"),Integer.parseInt(options.getOrDefault("https-port","8443")),Integer.parseInt(options.getOrDefault("quic-port","25575")),Integer.parseInt(options.getOrDefault("guest-port","25565")));
        Runtime.getRuntime().addShutdownHook(new Thread(relay::close));
        System.out.println("TFL relay ready; temporary assignments only. No public DNS is configured by this process.");
        new CountDownLatch(1).await();
    }
}
