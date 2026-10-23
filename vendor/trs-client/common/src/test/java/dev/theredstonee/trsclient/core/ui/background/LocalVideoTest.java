package dev.theredstonee.trsclient.core.ui.background;
import java.awt.image.BufferedImage;
import java.io.*;
import javax.imageio.ImageIO;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
class LocalVideoTest {
    byte[] jpeg() throws Exception {ByteArrayOutputStream b=new ByteArrayOutputStream();ImageIO.write(new BufferedImage(64,32,BufferedImage.TYPE_INT_RGB),"jpg",b);return b.toByteArray();}
    @Test void concatenatedBaselineFramesDecodeAndRespectAspectBudget() throws Exception {
        byte[] one=jpeg();ByteArrayOutputStream b=new ByteArrayOutputStream();b.write(one);b.write(one);
        ByteArrayInputStream in=new ByteArrayInputStream(b.toByteArray());
        assertArrayEquals(one,LocalMjpegPlayer.readFrame(in));assertArrayEquals(one,LocalMjpegPlayer.readFrame(in));assertNull(LocalMjpegPlayer.readFrame(in));
        BufferedImage image=LocalMjpegPlayer.decode(one,32,32);assertEquals(32,image.getWidth());assertEquals(16,image.getHeight());
    }
    @Test void embeddedEndMarkerInAppSegmentDoesNotTruncateVideo() throws Exception {
        byte[] one=jpeg();ByteArrayOutputStream b=new ByteArrayOutputStream();b.write(one,0,2);
        b.write(new byte[]{(byte)255,(byte)225,0,4,(byte)255,(byte)217});b.write(one,2,one.length-2);
        assertEquals(one.length+6,LocalMjpegPlayer.readFrame(new ByteArrayInputStream(b.toByteArray())).length);
    }
    @Test void truncatedAndInvalidStreamsFailClosed() {
        assertThrows(IOException.class,()->LocalMjpegPlayer.readFrame(new ByteArrayInputStream(new byte[]{1,2,3})));
        assertThrows(IOException.class,()->LocalMjpegPlayer.readFrame(new ByteArrayInputStream(new byte[]{(byte)255,(byte)216,(byte)255})));
    }
    @Test void workerLoopsLocalVideoAndClosesWithoutAGraphicsContext(@org.junit.jupiter.api.io.TempDir java.nio.file.Path dir) throws Exception {
        java.nio.file.Path file=dir.resolve("menu.mjpeg");java.nio.file.Files.write(file,jpeg());
        LocalMjpegPlayer player=new LocalMjpegPlayer(file);
        try {
            player.start(MenuBackground.Mode.BALANCED);
            long until=System.nanoTime()+3_000_000_000L;
            while(player.decodedFrames()<2 && System.nanoTime()<until)Thread.sleep(10);
            assertTrue(player.decodedFrames()>=2,"worker must decode and loop real frames");
            assertEquals(0,player.uploadedFrames(),"worker must never touch GPU");
        } finally {player.close();player.close();}
    }
}
