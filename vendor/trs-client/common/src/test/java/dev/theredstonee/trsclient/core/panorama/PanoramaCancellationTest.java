package dev.theredstonee.trsclient.core.panorama;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

class PanoramaCancellationTest {
  @Test
  void disablingBeforeCaptureReleasesThePendingRequest() {
    Panorama panorama = new Panorama();
    panorama.request(true, true);
    assertEquals(Panorama.POLL_NONE, panorama.poll(1000, true, false));
    assertTrue(panorama.cancelRequest());
    assertEquals(Panorama.State.IDLE, panorama.state());
    assertEquals(Panorama.POLL_NONE, panorama.poll(2000, true, false));
    assertFalse(panorama.cancelRequest());
    panorama.request(true, false);
    panorama.poll(3000, true, false);
    assertEquals(Panorama.POLL_CAPTURE, panorama.poll(3400, true, false));
    assertFalse(panorama.cancelRequest(), "Active captures must use their own restoration path");
    assertEquals(Panorama.State.CAPTURING, panorama.state());
  }
}
