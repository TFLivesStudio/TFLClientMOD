package dev.theredstonee.trsclient.core.ui.background;

import dev.theredstonee.trsclient.core.ui.TextureRef;
import dev.theredstonee.trsclient.core.ui.Textures;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.*;
import java.nio.file.*;
import java.util.Iterator;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.locks.LockSupport;
import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.stream.MemoryCacheImageInputStream;

/** Local baseline Motion JPEG video, 24 fps. No native decoder, external program, download or audio.
 * Worker owns file IO/decode/resize; only latest() uploads on the render thread. One pending frame maximum. */
public final class LocalMjpegPlayer implements MenuBackground.Player {
    private static final int MAX_FRAME=16*1024*1024;
    private final Path path;
    private volatile Work active;
    private TextureRef texture;
    private int generation=-1;
    private volatile long decoded;
    private long uploaded,uploadNanos,uploadBytes,uploadedFingerprint,sessions,startedNanos;
    private volatile long decodeNanos,decoderCpuNanos,loops,decodedFingerprint,discarded;
    private volatile String lastFailure="";
    private static final class Frame {
        final int w,h;final int[] pixels;final long fingerprint;
        Frame(BufferedImage image){w=image.getWidth();h=image.getHeight();pixels=image.getRGB(0,0,w,h,null,0,w);long hash=1469598103934665603L;for(int i=0;i<pixels.length;i+=Math.max(1,pixels.length/256))hash=(hash^pixels[i])*1099511628211L;fingerprint=hash;}
    }
    private static final class Work {
        final int sourceFps;final String resource;Work(int fps,String resource){sourceFps=fps;this.resource=resource;}
        volatile boolean running=true;volatile Throwable failure;volatile InputStream stream;Thread thread;
        final AtomicReference<Frame> pending=new AtomicReference<>();
    }
    public LocalMjpegPlayer(Path path){this.path=path;}
    public void start(final MenuBackground.Mode mode) throws IOException {
        close();
        if(Files.exists(path,LinkOption.NOFOLLOW_LINKS)) {
            if(!Files.isRegularFile(path,LinkOption.NOFOLLOW_LINKS)||Files.size(path)>256L*1024*1024)throw new IOException("Invalid local menu.mjpeg");
        } else {
            try(InputStream bundled=LocalMjpegPlayer.class.getResourceAsStream("/assets/trsclient/video/menu.mjpeg")) {
                if(bundled==null)throw new IOException("Missing bundled menu video");
            }
        }
        decoded=uploaded=uploadNanos=uploadBytes=decodeNanos=decoderCpuNanos=loops=discarded=0;decodedFingerprint=uploadedFingerprint=0;lastFailure="";sessions++;startedNanos=System.nanoTime();
        boolean custom=Files.exists(path,LinkOption.NOFOLLOW_LINKS);
        String resource=mode==MenuBackground.Mode.PERFORMANCE?"menu-performance.mjpeg":mode==MenuBackground.Mode.BALANCED?"menu-balanced.mjpeg":"menu.mjpeg";
        final Work work=new Work(!custom&&mode==MenuBackground.Mode.PERFORMANCE?15:24,resource);active=work;
        work.thread=new Thread(()->run(work,mode),"TFL-local-video");work.thread.setDaemon(true);work.thread.setPriority(Thread.MIN_PRIORITY);work.thread.start();
    }
    private InputStream openVideo(Work work) throws IOException {
        if(Files.exists(path,LinkOption.NOFOLLOW_LINKS)) {
            if(!Files.isRegularFile(path,LinkOption.NOFOLLOW_LINKS)||Files.size(path)>256L*1024*1024)throw new IOException("Invalid local video");
            return Files.newInputStream(path);
        }
        InputStream bundled=LocalMjpegPlayer.class.getResourceAsStream("/assets/trsclient/video/"+work.resource);
        if(bundled==null)throw new IOException("Missing bundled video");
        return bundled;
    }
    private void run(Work work,MenuBackground.Mode mode) {
        long due=System.nanoTime();
        int phase=work.sourceFps-Math.min(work.sourceFps,Math.max(1,mode.fps));
        try {
            while(work.running) {
                int count=0;
                try(InputStream in=new BufferedInputStream(openVideo(work))) {
                    work.stream=in;
                    byte[] jpeg;
                    while(work.running&&(jpeg=readFrame(in))!=null) {
                        count++;
                        long now=System.nanoTime();
                        phase+=Math.min(work.sourceFps,Math.max(1,mode.fps));
                        if(phase>=work.sourceFps) {
                            phase-=work.sourceFps;
                            long begin=System.nanoTime();
                            BufferedImage image=decode(jpeg,mode.width,mode.height);Frame frame=new Frame(image);
                            decodeNanos+=System.nanoTime()-begin;
                            java.lang.management.ThreadMXBean bean=java.lang.management.ManagementFactory.getThreadMXBean();if(bean.isCurrentThreadCpuTimeSupported())decoderCpuNanos=bean.getCurrentThreadCpuTime();
                            if(work.running){Frame old=work.pending.getAndSet(frame);if(old!=null)discarded++;decodedFingerprint=frame.fingerprint;decoded++;}

                        }
                        due+=1_000_000_000L/work.sourceFps;
                        long delay=due-System.nanoTime();
                        if(delay>0)LockSupport.parkNanos(delay);
                        else if(delay< -1_000_000_000L)due=System.nanoTime();
                        if(Thread.interrupted())work.running=false;
                    }
                } finally {work.stream=null;}
                if(count==0)throw new IOException("Empty video");
                if(work.running)loops++;
            }
        } catch(Exception | LinkageError e){if(work.running){work.failure=e;lastFailure=e.toString();}}
        finally {work.running=false;work.pending.set(null);}
    }
    /** Baseline JPEG marker/segment parser: APP metadata cannot be mistaken for frame delimiters. */
    static byte[] readFrame(InputStream in) throws IOException {
        int first=in.read();if(first<0)return null;
        if(first!=255||in.read()!=216)throw new IOException("Expected Motion JPEG SOI");
        ByteArrayOutputStream bytes=new ByteArrayOutputStream();bytes.write(255);bytes.write(216);
        boolean baseline=false;
        while(bytes.size()<MAX_FRAME) {
            int prefix=in.read(),marker=in.read();
            if(prefix!=255||marker<0)throw new IOException("Invalid JPEG marker");
            bytes.write(prefix);bytes.write(marker);
            if(marker==192)baseline=true;
            if(marker==194)throw new IOException("Progressive JPEG is unsupported");
            int hi=in.read(),lo=in.read();if(hi<0||lo<0)throw new EOFException();
            int length=(hi<<8)|lo;
            if(length<2||bytes.size()+length>MAX_FRAME)throw new IOException("Invalid JPEG segment");
            bytes.write(hi);bytes.write(lo);
            for(int i=2;i<length;i++){int v=in.read();if(v<0)throw new EOFException();bytes.write(v);}
            if(marker==218) {
                if(!baseline)throw new IOException("Baseline JPEG required");
                int previous=-1;
                while(bytes.size()<MAX_FRAME) {
                    int v=in.read();if(v<0)throw new EOFException();bytes.write(v);
                    if(previous==255&&v==217)return bytes.toByteArray();
                    previous=v;
                }
            }
        }
        throw new IOException("Video frame exceeds bound");
    }
    static BufferedImage decode(byte[] jpeg,int maxW,int maxH) throws IOException {
        try(MemoryCacheImageInputStream in=new MemoryCacheImageInputStream(new ByteArrayInputStream(jpeg))) {
            Iterator<ImageReader> readers=ImageIO.getImageReaders(in);
            if(!readers.hasNext())throw new IOException("No JPEG reader");
            ImageReader reader=readers.next();
            try {
                reader.setInput(in);int w=reader.getWidth(0),h=reader.getHeight(0);
                if(w<=0||h<=0||w>4096||h>4096||(long)w*h>8_388_608)throw new IOException("Video dimensions exceed bound");
                BufferedImage image=reader.read(0);
                double ratio=Math.min(1,Math.min(maxW/(double)w,maxH/(double)h));
                if(ratio>=1)return image;
                BufferedImage scaled=new BufferedImage(Math.max(1,(int)(w*ratio)),Math.max(1,(int)(h*ratio)),BufferedImage.TYPE_INT_RGB);
                Graphics2D g=scaled.createGraphics();try {g.setRenderingHint(RenderingHints.KEY_INTERPOLATION,RenderingHints.VALUE_INTERPOLATION_BILINEAR);g.drawImage(image,0,0,scaled.getWidth(),scaled.getHeight(),null);} finally {g.dispose();}
                return scaled;
            } finally {reader.dispose();}
        }
    }
    public TextureRef latest() {
        Work work=active;if(work==null)return null;
        if(work.failure!=null)throw new IllegalStateException("Local video failed",work.failure);
        Textures.Store store=Textures.store();if(store==null)return null;
        int current=store.resourceGeneration();if(current!=generation){texture=null;generation=current;}
        Frame frame=work.pending.getAndSet(null);
        if(frame!=null){long begin=System.nanoTime();texture=store.upload("menu-video",frame.w,frame.h,frame.pixels);uploadNanos+=System.nanoTime()-begin;
            if(texture==null)throw new IllegalStateException("Native video texture upload unavailable");uploaded++;uploadBytes+=(long)frame.w*frame.h*4;uploadedFingerprint=frame.fingerprint;}
        return texture;
    }
    public long uploadedSequence(){return uploaded;}
    public PlaybackMetrics metrics(){PlaybackMetrics m=new PlaybackMetrics();Work w=active;m.decoderState=w==null?"stopped":w.failure!=null?"failed":w.running?"running":"stopped";m.failure=lastFailure;m.sessions=sessions;m.framesDecoded=decoded;m.framesUploaded=uploaded;m.loops=loops;m.decodedFingerprint=decodedFingerprint;m.uploadedFingerprint=uploadedFingerprint;m.decodeNanos=decodeNanos;m.uploadNanos=uploadNanos;m.decoderCpuNanos=decoderCpuNanos;m.uploadBytes=uploadBytes;m.discardedFrames=discarded;m.pendingFrames=w!=null&&w.pending.get()!=null?1:0;m.ownedTextures=texture==null?0:1;m.elapsedNanos=startedNanos==0?0:System.nanoTime()-startedNanos;m.decoderFps=m.elapsedNanos<=0?0:decoded*1e9/m.elapsedNanos;return m;}
    public long decodedFrames(){return decoded;}
    public long uploadedFrames(){return uploaded;}
    public void close() {
        Work work=active;active=null;
        if(work!=null){work.running=false;work.thread.interrupt();try{if(work.stream!=null)work.stream.close();}catch(IOException ignored){}work.pending.set(null);}
        if(texture!=null&&Textures.store()!=null)Textures.store().release(texture);texture=null;
    }
}
