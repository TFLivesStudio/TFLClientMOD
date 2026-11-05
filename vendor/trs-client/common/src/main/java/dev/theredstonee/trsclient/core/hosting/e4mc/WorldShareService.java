package dev.theredstonee.trsclient.core.hosting.e4mc;

import dev.theredstonee.trsclient.core.hosting.LoginSniffer;
import dev.theredstonee.trsclient.core.hosting.net.PeerStream;
import java.io.*;
import java.net.*;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.*;

/** A consent-scoped relay into a real LAN listener. Never enables the unrelated TRS services. */
public final class WorldShareService {
    public static final String COMMUNITY_BROKER = "https://broker.e4mc.link/getBestRelay";
    static final int MAX_PEERS = 8, MAX_BYTES = 1024 * 1024;
    public enum State { OFF, PREPARING, CONNECTING, ONLINE, ERROR }
    public static final class Snapshot {
        public final State state;
        public final String address, error;
        public final int lanPort, peers, progress;
        Snapshot(State state, String address, String error, int port, int peers, int progress) {
            this.state=state; this.address=address; this.error=error; this.lanPort=port;
            this.peers=peers; this.progress=progress;
        }
    }
    /** Not persistent and not reusable. Checking this must be a deliberate UI action. */
    public static final class Consent {
        private boolean understood, used;
        public synchronized void understood(boolean value) { if (!used) understood=value; }
        public synchronized boolean ready() { return understood && !used; }
        private synchronized boolean consume() { if (!ready()) return false; used=true; return true; }
    }
    interface Factory { PublicTunnel create(Path path, E4mcRuntime.Progress progress) throws Exception; }
    private final Factory factory;
    private final Path runtime;
    // At most one preparation/cleanup operation, including when the UI is clicked repeatedly.
    private final ThreadPoolExecutor control = new ThreadPoolExecutor(0,1,1,TimeUnit.SECONDS,
        new ArrayBlockingQueue<Runnable>(8), threads("TFL-Share-Control"));
    private volatile Snapshot snapshot = new Snapshot(State.OFF,"","",0,0,0);
    private Session active;

    public WorldShareService(Path runtime) { this(runtime, E4mcRuntime::approvedShareTunnel); }
    WorldShareService(Path runtime, Factory factory) {
        this.runtime=runtime; this.factory=factory;
    }
    public synchronized Snapshot snapshot() {
        Snapshot s=snapshot;
        return active==null ? s : new Snapshot(s.state,s.address,s.error,s.lanPort,active.peers.size(),s.progress);
    }

    /** Native adapter supplies the live integrated-server identity and its actual published LAN port. */
    public synchronized boolean start(Object server, int port, String hostName, String broker, Consent consent) {
        if (server==null || port<1 || port>65535 || active!=null || control.getActiveCount()!=0
            || !control.getQueue().isEmpty() || consent==null || !consent.ready()) return false;
        URI uri;
        try { uri=URI.create(broker); } catch (RuntimeException e) { return false; }
        if (!"https".equalsIgnoreCase(uri.getScheme()) || uri.getHost()==null || uri.getUserInfo()!=null || uri.getFragment()!=null) return false;
        if (hostName==null || !hostName.matches("[A-Za-z0-9_]{1,16}") || !consent.consume()) return false;
        Session next = new Session(server,port,hostName,broker);
        active=next;
        update(next,State.PREPARING,"","",0);
        try { control.execute(next::connect); }
        catch (RejectedExecutionException e) { active=null; next.close(); snapshot=new Snapshot(State.ERROR,"","busy",port,0,0); return false; }
        return true;
    }
    /** Dimension travel keeps the same server. Leaving/replacing that server revokes the session. */
    public synchronized void tick(Object currentServer) {
        if (active!=null && active.server!=currentServer) stop();
    }
    public synchronized void stop() {
        Session old=active;
        active=null;
        snapshot=new Snapshot(State.OFF,"","",old==null ? snapshot.lanPort : old.port,0,0);
        if (old!=null) {
            old.close(); // closes local sockets and cancels queued work; no network waits on UI thread
            try { control.execute(old::stopTunnel); } catch (RejectedExecutionException ignored) {
                // Queue has at most eight operations; live work checks closed before starting a tunnel.
            }
        }
    }
    private synchronized void update(Session s, State state, String address, String error, int progress) {
        if (active==s && !s.closed.get()) snapshot=new Snapshot(state,address,error,s.port,s.peers.size(),progress);
    }
    private synchronized void fail(Session s, String error) {
        if (active!=s || s.closed.get()) return;
        snapshot=new Snapshot(State.ERROR,"",error,s.port,0,0);
        active=null; s.close();
        try { control.execute(s::stopTunnel); } catch (RejectedExecutionException ignored) { }
    }
    private static ThreadFactory threads(final String name) {
        final AtomicInteger ids=new AtomicInteger();
        return r -> { Thread t=new Thread(r,name+"-"+ids.incrementAndGet()); t.setDaemon(true); return t; };
    }
    private final class Session implements PublicTunnel.Events {
        final Object server;
        final int port;
        final String host, broker;
        final AtomicBoolean closed=new AtomicBoolean();
        final Set<Bridge> peers=Collections.newSetFromMap(new ConcurrentHashMap<Bridge,Boolean>());
        final ThreadPoolExecutor io=new ThreadPoolExecutor(0,MAX_PEERS*2,1,TimeUnit.SECONDS,
            new SynchronousQueue<Runnable>(),threads("TFL-Share-Guest"));
        volatile PublicTunnel tunnel;
        Session(Object server,int port,String host,String broker) { this.server=server; this.port=port; this.host=host; this.broker=broker; }
        void connect() {
            try {
                if (closed.get()) return;
                PublicTunnel created=factory.create(runtime,p -> update(this,State.PREPARING,"","",p));
                tunnel=created;
                if (closed.get()) { created.stop(); return; }
                update(this,State.CONNECTING,"","",100);
                created.start(broker,this);
                if (closed.get()) created.stop();
            } catch (Exception | LinkageError e) { fail(this, hasTlsCause(e) ? "tls" : "connection"); stopTunnel(); }
        }
        public void domain(String domain) {
            if (domain==null || domain.length()>253 || !domain.matches("[A-Za-z0-9](?:[A-Za-z0-9.-]*[A-Za-z0-9])?")) { fail(this,"address"); return; }
            update(this,State.ONLINE,domain,"",100);
        }
        public void broadcast(String message) { /* Never render untrusted relay markup or open links. */ }
        public void failed(String reason) { fail(this,"connection"); }
        public synchronized void stream(PeerStream peer) {
            if (closed.get() || snapshot.state!=State.ONLINE || peers.size()>=MAX_PEERS) { peer.close("sharing unavailable"); return; }
            Bridge b=new Bridge(this,peer); peers.add(b);
            try { peer.start(b); io.execute(b::writeLan); }
            catch (RuntimeException e) { b.close(); }
        }
        void close() { if (closed.compareAndSet(false,true)) { for (Bridge p:peers) p.close(); io.shutdownNow(); } }
        void stopTunnel() { PublicTunnel t=tunnel; if (t!=null) t.stop(); }
    }
    private static boolean hasTlsCause(Throwable e) {
        for (int i=0;e!=null && i<16;i++,e=e.getCause()) if (e instanceof javax.net.ssl.SSLException || e instanceof java.security.cert.CertificateException) return true;
        return false;
    }
    private final class Bridge implements PeerStream.Sink {
        final Session session;
        final PeerStream peer;
        final AtomicBoolean closed=new AtomicBoolean();
        final AtomicInteger pending=new AtomicInteger();
        final ArrayBlockingQueue<byte[]> input=new ArrayBlockingQueue<byte[]>(64);
        final LoginSniffer sniffer;
        volatile Socket socket;
        Bridge(Session session,PeerStream peer) {
            this.session=session; this.peer=peer;
            sniffer=new LoginSniffer(name -> !session.host.equalsIgnoreCase(name));
        }
        public void data(byte[] data,int offset,int length) {
            if (closed.get() || length<=0) return;
            if (length>MAX_BYTES || pending.addAndGet(length)>MAX_BYTES) { close(); return; }
            byte[] copy=Arrays.copyOfRange(data,offset,offset+length);
            if (!input.offer(copy)) { pending.addAndGet(-length); close(); }
        }
        public void closed(String reason) { close(); }
        void writeLan() {
            try {
                ByteArrayOutputStream prefix=new ByteArrayOutputStream();
                OutputStream out=null;
                while (!closed.get() && !session.closed.get()) {
                    byte[] data=input.poll(10,TimeUnit.SECONDS);
                    if (data==null) { if (out==null || !peer.isOpen()) break; continue; }
                    pending.addAndGet(-data.length);
                    if (out==null) {
                        LoginSniffer.Verdict verdict=sniffer.feed(data,0,data.length);
                        if (verdict==LoginSniffer.Verdict.REJECT) break;
                        if (prefix.size()+data.length>MAX_BYTES) break;
                        prefix.write(data);
                        if (verdict==LoginSniffer.Verdict.MORE) continue;
                        Socket local=new Socket(); socket=local;
                        if (closed.get()) { local.close(); break; }
                        local.connect(new InetSocketAddress("127.0.0.1",session.port),3000);
                        local.setTcpNoDelay(true);
                        out=local.getOutputStream(); prefix.writeTo(out); prefix=null;
                        session.io.execute(this::readLan);
                    } else out.write(data);
                }
            } catch (IOException | InterruptedException | RejectedExecutionException ignored) {
                if (Thread.currentThread().isInterrupted()) Thread.currentThread().interrupt();
            } finally { close(); }
        }
        void readLan() {
            try {
                byte[] bytes=new byte[16384]; InputStream in=socket.getInputStream();
                int n;
                while (!closed.get() && (n=in.read(bytes))>=0) {
                    while (!closed.get() && peer.pendingBytes()>MAX_BYTES/2) TimeUnit.MILLISECONDS.sleep(5);
                    if (closed.get() || !peer.write(bytes,0,n)) break;
                }
            } catch (IOException | InterruptedException ignored) {
                if (Thread.currentThread().isInterrupted()) Thread.currentThread().interrupt();
            } finally { close(); }
        }
        void close() {
            if (!closed.compareAndSet(false,true)) return;
            Socket local=socket;
            if (local!=null) try { local.close(); } catch (IOException ignored) { }
            input.clear(); peer.close("sharing closed"); session.peers.remove(this);
        }
    }
}
