package dev.theredstonee.trsclient.core.ui.menu;

import dev.theredstonee.trsclient.core.i18n.I18n;
import dev.theredstonee.trsclient.core.ui.clips.ClipsHost;
import dev.theredstonee.trsclient.core.ui.clips.ClipsUi;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

/** Exercise the actual gallery with a local screenshot and no launcher or service provider. */
class LocalGalleryNavigationTest {
    @Test void localScreenshotCanBeBrowsedWithoutAdvertisingUnavailableActions(@TempDir Path dir) throws Exception {
        I18n.use("en");
        Path screenshots = Files.createDirectories(dir.resolve("screenshots"));
        ImageIO.write(new BufferedImage(16, 9, BufferedImage.TYPE_INT_RGB), "png", screenshots.resolve("local-shot.png").toFile());
        final boolean[] closed = {false};
        ClipsUi gallery = new ClipsUi(new ClipsHost() {
            public void playClick() {}
            public void closeScreen() { closed[0] = true; }
            public Path gameDir() { return dir; }
            public Path configDir() { return dir.resolve("config"); }
        });
        MenuRailTest.RecordingCanvas c = new MenuRailTest.RecordingCanvas();
        try {
            long deadline = System.currentTimeMillis() + 3000;
            while (gallery.alpha() < .99f || c.visible("local-shot") == null) {
                gallery.render(c, 960, 540, -100, -100);
                if (System.currentTimeMillis() > deadline) fail("Local screenshot was not listed");
                Thread.sleep(15);
            }
            for (int[] size : new int[][]{{320, 200}, {960, 540}}) {
                c.clear();
                gallery.render(c, size[0], size[1], -100, -100);
                assertNotNull(c.visible(I18n.tr("clips.filter.screenshots", 1)));
                assertNotNull(c.visible(I18n.tr("clips.filter.favorites", 0)));
                assertNull(c.visible(I18n.tr("clips.filter.shared")));
                assertNull(c.visible(I18n.tr("clips.record")));
                assertNull(c.visible(I18n.tr("clips.save")));
                assertNull(c.visible(I18n.tr("clips.enable.button")));
                assertNull(c.visible(I18n.tr("clips.link.none")));
            }
            gallery.openFirstImage();
            c.clear();
            gallery.render(c, 960, 540, -100, -100);
            assertNotNull(c.visible("local-shot.png"));
            assertNull(c.visible(I18n.tr("clips.share.button")));
            assertTrue(Files.isRegularFile(screenshots.resolve("local-shot.png")));
        } finally {
            gallery.requestClose();
            long deadline = System.currentTimeMillis() + 1000;
            while (!closed[0] && System.currentTimeMillis() < deadline) {
                gallery.render(c, 960, 540, -100, -100);
                Thread.sleep(15);
            }
        }
    }
}
