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
    private long uploaded;
    private static final class Frame {
        final int w,h;final int[] pixels;
        Frame(BufferedImage image){w=image.getWidth();h=image.getHeight();pixels=image.getRGB(0,0,w,h,null,0,w);}
    }
    private static final class Work {
        volatile boolean running=true;volatile Throwable failure;volatile InputStream stream;Thread thread;
        final AtomicReference<Frame> pending=new AtomicReference<>();
    }
    public LocalMjpegPlayer(Path path){this.path=path;}
    public void start(final MenuBackground.Mode mode) throws IOException {
        close();
        if(!Files.isRegularFile(path,LinkOption.NOFOLLOW_LINKS)||Files.size(path)>256L*1024*1024)throw new IOException("Missing/oversized local menu.mjpeg");
        final Work work=new Work();active=work;
        work.thread=new Thread(()->run(work,mode),"TFL-local-video");work.thread.setDaemon(true);work.thread.setPriority(Thread.MIN_PRIORITY);work.thread.start();
    }
    private void run(Work work,MenuBackground.Mode mode) {
        long due=System.nanoTime(),lastOutput=Long.MIN_VALUE/2;
        try {
            while(work.running) {
                int count=0;
                try(InputStream in=new BufferedInputStream(Files.newInputStream(path))) {
                    work.stream=in;
                    byte[] jpeg;
                    while(work.running&&(jpeg=readFrame(in))!=null) {
                        count++;
                        long now=System.nanoTime();
                        if(now-lastOutput>=1_000_000_000L/Math.max(1,mode.fps)) {
                            BufferedImage image=decode(jpeg,mode.width,mode.height);
                            if(work.running){work.pending.set(new Frame(image));decoded++;}
                            lastOutput=now;
                        }
                        due+=1_000_000_000L/24;
                        long delay=due-System.nanoTime();
                        if(delay>0)LockSupport.parkNanos(delay);
                        else if(delay< -1_000_000_000L)due=System.nanoTime();
                        if(Thread.interrupted())work.running=false;
                    }
                } finally {work.stream=null;}
                if(count==0)throw new IOException("Empty video");
            }
        } catch(Exception | LinkageError e){if(work.running)work.failure=e;}
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
        if(frame!=null){texture=store.upload("menu-video",frame.w,frame.h,frame.pixels);uploaded++;}
        return texture;
    }
    public long decodedFrames(){return decoded;}
    public long uploadedFrames(){return uploaded;}
    public void close() {
        Work work=active;active=null;
        if(work!=null){work.running=false;work.thread.interrupt();try{if(work.stream!=null)work.stream.close();}catch(IOException ignored){}work.pending.set(null);}
        if(texture!=null&&Textures.store()!=null)Textures.store().release(texture);texture=null;
    }
}
