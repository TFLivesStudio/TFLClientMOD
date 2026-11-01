package dev.theredstonee.trsclient.ornithe.dev;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class GlSnapshotTest {
  @TempDir Path temporary;

  @Test
  void captureIsInertWhileDisarmed() {
    // The diagnostic must stay invisible for every non-probe run: no throws, no writes.
    assertDoesNotThrow(() -> GlSnapshot.capture("disabled"));
    try (var files = Files.list(temporary)) {
      assertEquals(0, files.count());
    } catch (Exception io) {
      throw new AssertionError(io);
    }
  }

  @Test
  void armedCaptureNeverThrowsWithoutAnOpenGlContext() {
    // Guarded reads keep the render loop safe even when every GL query fails, which is the
    // contract that lets the snapshot ride inside the production HUD hook.
    GlSnapshot.enable(temporary, () -> 4);
    assertDoesNotThrow(() -> GlSnapshot.capture("test"));
    assertDoesNotThrow(() -> GlSnapshot.capture("test-again"));
  }
}
