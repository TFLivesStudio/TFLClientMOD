package dev.theredstonee.trsclient.core.ui.background;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import java.nio.file.Path;
import java.io.InputStream;
import static org.junit.jupiter.api.Assertions.*;
class BundledVideoTest {
    @Test void shippedVideoContainsRealFullHdBaselineFrames() throws Exception {
        try(InputStream in=getClass().getResourceAsStream("/assets/trsclient/video/menu.mjpeg")){
            assertNotNull(in);
            byte[] first=LocalMjpegPlayer.readFrame(in),second=LocalMjpegPlayer.readFrame(in);
            assertNotNull(first);assertNotNull(second);assertFalse(java.util.Arrays.equals(first,second));
            java.awt.image.BufferedImage frame=LocalMjpegPlayer.decode(first,1920,1080);
            assertEquals(1920,frame.getWidth());assertEquals(1080,frame.getHeight());
        }
    }
    @Test void cleanInstallDecodesBundledVideoWithoutLocalFile(@TempDir Path dir) throws Exception {
        LocalMjpegPlayer player=new LocalMjpegPlayer(dir.resolve("menu.mjpeg"));
        try{
            player.start(MenuBackground.Mode.PERFORMANCE);
            long deadline=System.nanoTime()+5_000_000_000L;
            while(player.decodedFrames()<2&&System.nanoTime()<deadline)Thread.sleep(10);
            assertTrue(player.decodedFrames()>=2);assertEquals(0,player.uploadedFrames());
        }finally{player.close();}
    }
}
