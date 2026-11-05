package dev.theredstonee.trsclient.core.ui.background;

import static org.junit.jupiter.api.Assertions.*;
import java.awt.image.BufferedImage;
import java.io.*;
import java.nio.file.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicReference;
import java.util.jar.*;
import javax.imageio.ImageIO;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class VideoStreamOwnershipTest {
    @TempDir Path temp;

    @Test void stoppingVideoLeavesCompressedJarStreamClosureToItsReader() throws Exception {
        ByteArrayOutputStream jpeg=new ByteArrayOutputStream();
        ImageIO.write(new BufferedImage(16,16,BufferedImage.TYPE_INT_RGB),"jpeg",jpeg);
        Path archive=temp.resolve("client.jar");
        byte[] other=new byte[8192];new java.util.Random(189).nextBytes(other);
        try(JarOutputStream out=new JarOutputStream(Files.newOutputStream(archive))) {
            out.putNextEntry(new JarEntry("menu.mjpeg"));out.write(jpeg.toByteArray());out.closeEntry();
            out.putNextEntry(new JarEntry("other.class"));out.write(other);out.closeEntry();
        }
        Path local=temp.resolve("menu.mjpeg");Files.write(local,jpeg.toByteArray());
        CountDownLatch reading=new CountDownLatch(1),release=new CountDownLatch(1),closed=new CountDownLatch(1);
        AtomicReference<Thread> reader=new AtomicReference<>(),closer=new AtomicReference<>();
        try(JarFile jar=new JarFile(archive.toFile())) {
            LocalMjpegPlayer player=new LocalMjpegPlayer(local,()->new FilterInputStream(jar.getInputStream(jar.getJarEntry("menu.mjpeg"))) {
                @Override public int read(byte[] b,int off,int len) throws IOException {
                    reader.set(Thread.currentThread());reading.countDown();
                    boolean interrupted=false;
                    for(;;)try{release.await();break;}catch(InterruptedException e){interrupted=true;}
                    if(interrupted){Thread.currentThread().interrupt();throw new InterruptedIOException("cancelled fixture read");}
                    return super.read(b,off,len);
                }
                @Override public void close() throws IOException {
                    closer.set(Thread.currentThread());try{super.close();}finally{closed.countDown();}
                }
            });
            try {
                player.start(MenuBackground.Mode.HIGH);
                assertTrue(reading.await(5,TimeUnit.SECONDS),"Video reader did not start");
                player.close();
                assertEquals(1,closed.getCount(),"UI must not release an inflater while the video reader still uses it");
                try(InputStream input=jar.getInputStream(jar.getJarEntry("other.class"))) {
                    assertArrayEquals(other,input.readAllBytes(),"Another compressed class entry remains readable during cancellation");
                }
            } finally {
                release.countDown();player.close();
                assertTrue(closed.await(5,TimeUnit.SECONDS),"Reader must close its own stream after cancellation");
            }
            assertSame(reader.get(),closer.get(),"Compressed stream must be closed by its owning reader thread");
        }
    }
}
