package dev.theredstonee.trsclient.core.ui.background;

import dev.theredstonee.trsclient.core.ui.TextureRef;
import dev.theredstonee.trsclient.core.ui.Textures;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.*;
import java.nio.file.*;
import java.util.Iterator;
import java.util.concurrent.locks.LockSupport;
import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.stream.MemoryCacheImageInputStream;

/** Local baseline Motion JPEG video, 30 fps. Uses a bundled native codec when supplied, otherwise ImageIO. No downloads or audio.
 * Worker owns file IO/decode/resize; only latest() uploads on the render thread. At most two queued frames absorb render-thread jitter without an unbounded backlog. */
public final class LocalMjpegPlayer implements MenuBackground.Player {
    /** Uses a game's bundled codec; never installs or downloads a decoder. */
    public interface NativeDecoder {
        PixelFrame decode(byte[] jpeg,int maxWidth,int maxHeight,int[] storage) throws IOException;
    }
    public static final class PixelFrame {
        public final int width,height;public final int[] pixels;
        public PixelFrame(int width,int height,int[] pixels){this.width=width;this.height=height;this.pixels=pixels;}
    }
    private static volatile NativeDecoder nativeDecoder;
    public static void installNativeDecoder(NativeDecoder decoder){nativeDecoder=decoder;}
    private static final int MAX_FRAME=16*1024*1024;
    private final Path path;
    interface VideoSource { InputStream open() throws IOException; }
    private final VideoSource source;
    private volatile Work active;
    private TextureRef texture;
    private int generation=-1;
    private volatile long decoded;
    private long uploaded,uploadNanos,uploadBytes,uploadedFingerprint,sessions,startedNanos;
    private volatile long decodeNanos,decoderCpuNanos,loops,decodedFingerprint,discarded;
    private volatile String lastFailure="";
    private static final class Frame {
        final int w,h;final int[] pixels;final long fingerprint;
        Frame(PixelFrame frame){w=frame.width;h=frame.height;pixels=frame.pixels;long hash=1469598103934665603L;for(int i=0;i<w*h;i+=Math.max(1,w*h/256))hash=(hash^pixels[i])*1099511628211L;fingerprint=hash;}
        Frame(BufferedImage image,int[] storage){w=image.getWidth();h=image.getHeight();pixels=storage;
            if(image.getType()==BufferedImage.TYPE_INT_RGB && image.getRaster().getDataBuffer() instanceof java.awt.image.DataBufferInt){
                int[] source=((java.awt.image.DataBufferInt)image.getRaster().getDataBuffer()).getData();
                for(int i=0;i<w*h;i++)pixels[i]=source[i]|0xFF000000;
            }else if(image.getType()==BufferedImage.TYPE_3BYTE_BGR&&image.getRaster().getDataBuffer() instanceof java.awt.image.DataBufferByte){
                byte[] bgr=((java.awt.image.DataBufferByte)image.getRaster().getDataBuffer()).getData();
                for(int i=0,j=0;i<w*h;i++,j+=3)pixels[i]=0xff000000|(bgr[j+2]&255)<<16|(bgr[j+1]&255)<<8|(bgr[j]&255);
            }else image.getRGB(0,0,w,h,storage,0,w);long hash=1469598103934665603L;for(int i=0;i<pixels.length;i+=Math.max(1,pixels.length/256))hash=(hash^pixels[i])*1099511628211L;fingerprint=hash;}
    }
    private static final class Work {
        final int sourceFps;final String resource;Work(int fps,String resource){sourceFps=fps;this.resource=resource;decoder=ThreadLocal.withInitial(()->{Work local=new Work(sourceFps,this.resource);decoders.add(local);return local;});}
        final java.util.List<Work> decoders=java.util.Collections.synchronizedList(new java.util.ArrayList<Work>());
        final ThreadLocal<Work> decoder;
        volatile java.util.concurrent.ExecutorService executor;
        volatile boolean running=true;volatile Throwable failure;Thread thread;
        final java.util.concurrent.ArrayBlockingQueue<Frame> pending=new java.util.concurrent.ArrayBlockingQueue<>(2);
        final java.util.ArrayDeque<int[]> pool=new java.util.ArrayDeque<int[]>();
        BufferedImage sourceImage,scaledImage;volatile long pixelAllocations,imageAllocations,cpuNanos;
        synchronized int[] acquire(int count){while(!pool.isEmpty()){int[] p=pool.removeFirst();if(p.length==count)return p;}pixelAllocations++;return new int[count];}
        synchronized void recycle(Frame frame){if(frame!=null&&pool.size()<10)pool.addLast(frame.pixels);}

    }
    public LocalMjpegPlayer(Path path){this(path,null);}
    LocalMjpegPlayer(Path path,VideoSource source){this.path=path;this.source=source;}
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
        final Work work=new Work(custom?24:30,resource);active=work;
        work.thread=new Thread(()->run(work,mode),"TFL-local-video");work.thread.setDaemon(true);work.thread.setPriority(Thread.NORM_PRIORITY);work.thread.start();
    }
    private InputStream openVideo(Work work) throws IOException {
        if(source!=null)return source.open();
        if(Files.exists(path,LinkOption.NOFOLLOW_LINKS)) {
            if(!Files.isRegularFile(path,LinkOption.NOFOLLOW_LINKS)||Files.size(path)>256L*1024*1024)throw new IOException("Invalid local video");
            return Files.newInputStream(path);
        }
        InputStream bundled=LocalMjpegPlayer.class.getResourceAsStream("/assets/trsclient/video/"+work.resource);
        if(bundled==null)throw new IOException("Missing bundled video");
        return bundled;
    }
    private void run(Work work,MenuBackground.Mode mode) {
        java.util.concurrent.ExecutorService executor=java.util.concurrent.Executors.newFixedThreadPool(3,r->{Thread t=new Thread(r,"TFL-video-decode");t.setDaemon(true);return t;});
        work.executor=executor;
        java.util.ArrayDeque<java.util.concurrent.Future<Frame>> ahead=new java.util.ArrayDeque<>();
        long due=0;
        try {
            while(work.running) {
                int count=0;
                try(InputStream in=new BufferedInputStream(openVideo(work),256*1024)) {
                    boolean eof=false;
                    while(work.running&&(!eof||!ahead.isEmpty())) {
                        while(work.running&&!eof&&ahead.size()<6) {
                            final byte[] jpeg=readFrame(in);
                            if(jpeg==null){eof=true;break;}
                            count++;
                            ahead.add(executor.submit(()->{
                                long begin=System.nanoTime();
                                Work decoder=work.decoder.get();
                                Frame frame;
                                NativeDecoder accelerated=nativeDecoder;
                                if(accelerated!=null){
                                    int[] pixels=work.acquire(mode.width*mode.height);
                                    frame=new Frame(accelerated.decode(jpeg,mode.width,mode.height,pixels));
                                }else{
                                    BufferedImage image=decode(jpeg,mode.width,mode.height,decoder);
                                    frame=new Frame(image,work.acquire(image.getWidth()*image.getHeight()));
                                }
                                java.lang.management.ThreadMXBean bean=java.lang.management.ManagementFactory.getThreadMXBean();if(bean.isCurrentThreadCpuTimeSupported())decoder.cpuNanos=bean.getCurrentThreadCpuTime();
                                synchronized(work){if(active==work&&work.running)decodeNanos+=System.nanoTime()-begin;}
                                return frame;
                            }));
                        }
                        if(ahead.isEmpty())break;
                        Frame frame=ahead.removeFirst().get();
                        if(due==0)due=System.nanoTime();
                        long delay=due-System.nanoTime();
                        if(delay>0)LockSupport.parkNanos(delay);
                        if(!work.running||Thread.interrupted()){work.recycle(frame);break;}
                        if(!work.pending.offer(frame)){Frame old=work.pending.poll();if(old!=null){discarded++;work.recycle(old);}work.pending.offer(frame);}
                        decodedFingerprint=frame.fingerprint;decoded++;
                        due+=1_000_000_000L/work.sourceFps;
                        // Never stall the render thread or build an unbounded backlog.
                        if(due<System.nanoTime()-1_000_000_000L)due=System.nanoTime();
                    }
                }
                if(count==0)throw new IOException("Empty video");
                if(work.running)loops++;
            }
        } catch(Exception | LinkageError e){if(work.running){work.failure=e;lastFailure=e.toString();}}
        finally {work.running=false;executor.shutdownNow();work.pending.clear();}
    }
    /** Baseline JPEG marker/segment parser: APP metadata cannot be mistaken for frame delimiters. */
    static byte[] readFrame(InputStream in) throws IOException {
        checkCancelled();
        int first=in.read();if(first<0)return null;
        if(first!=255||in.read()!=216)throw new IOException("Expected Motion JPEG SOI");
        ByteArrayOutputStream bytes=new ByteArrayOutputStream();bytes.write(255);bytes.write(216);
        boolean baseline=false;
        while(bytes.size()<MAX_FRAME) {
            checkCancelled();
            int prefix=in.read(),marker=in.read();
            if(prefix!=255||marker<0)throw new IOException("Invalid JPEG marker");
            bytes.write(prefix);bytes.write(marker);
            if(marker==192)baseline=true;
            if(marker==194)throw new IOException("Progressive JPEG is unsupported");
            int hi=in.read(),lo=in.read();if(hi<0||lo<0)throw new EOFException();
            int length=(hi<<8)|lo;
            if(length<2||bytes.size()+length>MAX_FRAME)throw new IOException("Invalid JPEG segment");
            bytes.write(hi);bytes.write(lo);
            for(int i=2;i<length;i++){if((i&16383)==0)checkCancelled();int v=in.read();if(v<0)throw new EOFException();bytes.write(v);}
            if(marker==218) {
                if(!baseline)throw new IOException("Baseline JPEG required");
                int previous=-1;
                while(bytes.size()<MAX_FRAME) {
                    if((bytes.size()&16383)==0)checkCancelled();
                    int v=in.read();if(v<0)throw new EOFException();bytes.write(v);
                    if(previous==255&&v==217)return bytes.toByteArray();
                    previous=v;
                }
            }
        }
        throw new IOException("Video frame exceeds bound");
    }
    private static void checkCancelled() throws InterruptedIOException {
        if(Thread.currentThread().isInterrupted())throw new InterruptedIOException("Video reading cancelled");
    }
    static BufferedImage decode(byte[] jpeg,int maxW,int maxH) throws IOException {return decode(jpeg,maxW,maxH,null);}
    private static BufferedImage decode(byte[] jpeg,int maxW,int maxH,Work work) throws IOException {
        try(MemoryCacheImageInputStream in=new MemoryCacheImageInputStream(new ByteArrayInputStream(jpeg))) {
            Iterator<ImageReader> readers=ImageIO.getImageReaders(in);
            if(!readers.hasNext())throw new IOException("No JPEG reader");
            ImageReader reader=readers.next();
            try {
                reader.setInput(in);int w=reader.getWidth(0),h=reader.getHeight(0);
                if(w<=0||h<=0||w>4096||h>4096||(long)w*h>8_388_608)throw new IOException("Video dimensions exceed bound");
                javax.imageio.ImageReadParam parameters=reader.getDefaultReadParam();
                if(work!=null&&work.sourceImage==null){
                    Iterator<javax.imageio.ImageTypeSpecifier> types=reader.getImageTypes(0);
                    while(types.hasNext()){javax.imageio.ImageTypeSpecifier type=types.next();if(type.getBufferedImageType()==BufferedImage.TYPE_3BYTE_BGR){work.sourceImage=type.createBufferedImage(w,h);work.imageAllocations++;break;}}
                }
                if(work!=null&&work.sourceImage!=null&&work.sourceImage.getWidth()==w&&work.sourceImage.getHeight()==h)parameters.setDestination(work.sourceImage);
                BufferedImage image;
                try{image=reader.read(0,parameters);}catch(IllegalArgumentException incompatible){parameters.setDestination(null);image=reader.read(0,parameters);}
                if(work!=null){if(image!=work.sourceImage)work.imageAllocations++;work.sourceImage=image;}
                double ratio=Math.min(1,Math.min(maxW/(double)w,maxH/(double)h));
                if(ratio>=1)return image;
                int sw=Math.max(1,(int)(w*ratio)),sh=Math.max(1,(int)(h*ratio));
                BufferedImage scaled=work==null?null:work.scaledImage;
                if(scaled==null||scaled.getWidth()!=sw||scaled.getHeight()!=sh){scaled=new BufferedImage(sw,sh,BufferedImage.TYPE_INT_RGB);if(work!=null){work.scaledImage=scaled;work.imageAllocations++;}}
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
        Frame frame=work.pending.poll();
        if(frame!=null){long begin=System.nanoTime();try{texture=store.upload("menu-video",frame.w,frame.h,frame.pixels);}finally{work.recycle(frame);}uploadNanos+=System.nanoTime()-begin;
            if(texture==null)throw new IllegalStateException("Native video texture upload unavailable");uploaded++;uploadBytes+=(long)frame.w*frame.h*4;uploadedFingerprint=frame.fingerprint;}
        return texture;
    }
    public long uploadedSequence(){return uploaded;}
    public PlaybackMetrics metrics(){PlaybackMetrics m=new PlaybackMetrics();Work w=active;m.pixelBufferAllocations=w==null?0:w.pixelAllocations;m.imageBufferAllocations=0;if(w!=null)synchronized(w.decoders){for(Work decoder:w.decoders){m.imageBufferAllocations+=decoder.imageAllocations;m.decoderCpuNanos+=decoder.cpuNanos;}}m.decoderImplementation=nativeDecoder==null?"ImageIO parallel":"Minecraft native JPEG parallel";m.decoderState=w==null?"stopped":w.failure!=null?"failed":w.running?"running":"stopped";m.failure=lastFailure;m.sessions=sessions;m.framesDecoded=decoded;m.framesUploaded=uploaded;m.loops=loops;m.decodedFingerprint=decodedFingerprint;m.uploadedFingerprint=uploadedFingerprint;m.decodeNanos=decodeNanos;m.uploadNanos=uploadNanos;m.uploadBytes=uploadBytes;m.discardedFrames=discarded;m.pendingFrames=w==null?0:w.pending.size();m.ownedTextures=texture==null?0:1;m.elapsedNanos=startedNanos==0?0:System.nanoTime()-startedNanos;m.decoderFps=w==null||m.elapsedNanos<=0?0:decoded*1e9/m.elapsedNanos;return m;}
    public long decodedFrames(){return decoded;}
    public long uploadedFrames(){return uploaded;}
    public void close() {
        Work work=active;active=null;
        // The worker owns the compressed JAR stream. Closing it from this thread can
        // return its inflater to JarFile while a read is still in progress. Interrupt
        // bounded local parsing instead; try-with-resources closes on the owner thread.
        if(work!=null){work.running=false;if(work.executor!=null)work.executor.shutdownNow();work.thread.interrupt();work.pending.clear();}
        if(texture!=null&&Textures.store()!=null)Textures.store().release(texture);texture=null;
    }
}
