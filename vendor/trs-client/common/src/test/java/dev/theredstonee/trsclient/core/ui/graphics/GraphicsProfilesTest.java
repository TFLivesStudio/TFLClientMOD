package dev.theredstonee.trsclient.core.ui.graphics;

import static org.junit.jupiter.api.Assertions.*;

import java.util.*;
import org.junit.jupiter.api.Test;

class GraphicsProfilesTest {
  enum Clouds {
    OFF,
    FAST,
    FANCY
  }

  static class Binding implements GraphicsUi.Option, GraphicsProfiles.Capability {
    final String id;
    Object value, saved;
    boolean enabled = true, reject, fail;
    int writes;

    Binding(String id, Object value) {
      this.id = id;
      this.value = value;
      this.saved = value;
    }

    public String id() {
      return id;
    }

    public String name() {
      return id;
    }

    public String description() {
      return "";
    }

    public String value() {
      return String.valueOf(value);
    }

    public int category() {
      return 1;
    }

    public boolean available() {
      return enabled;
    }

    public boolean changed() {
      return !Objects.equals(value, saved);
    }

    public void step(int n) {
      throw new AssertionError("presets must not cycle native values");
    }

    public void revert() {
      value = saved;
    }

    public GraphicsProfiles.Capability capability() {
      return this;
    }

    public Object read() {
      return value;
    }

    public boolean accepts(Object v) {
      return !reject && v != null && v.getClass() == value.getClass();
    }

    public Object[] choices() {
      return value instanceof Clouds ? Clouds.values() : new Object[0];
    }

    public void write(Object v) {
      writes++;
      value = v;
      if (fail) {
        fail = false;
        throw new IllegalStateException("write failed after mutation");
      }
    }
  }

  static class Host implements GraphicsUi.Host {
    final List<GraphicsUi.Option> options = new ArrayList<>();
    int saves;
    boolean failSave;
    String confirmation = "";

    Binding add(String id, Object v) {
      Binding b = new Binding(id, v);
      options.add(b);
      return b;
    }

    public List<GraphicsUi.Option> options() {
      return options;
    }

    public String applyConfirmation() {
      return confirmation;
    }

    public void apply() {
      saves++;
      if (failSave) {
        failSave = false;
        throw new IllegalStateException("save failed");
      }
      for (GraphicsUi.Option o : options) ((Binding) o).saved = ((Binding) o).value;
    }

    public void close() {}

    public void shaders() {}

    public boolean hasShaders() {
      return false;
    }

    public String status() {
      return "";
    }
  }

  @Test
  void previewDoesNotMutateAndApplyUsesNativeTypedValuesThenUndoRestoresSnapshot() {
    Host h = new Host();
    Binding distance = h.add("minecraft:render_distance", 8),
        clouds = h.add("minecraft:clouds", Clouds.OFF);
    GraphicsProfiles p = new GraphicsProfiles(h);
    GraphicsProfiles.Plan plan = p.prepare(GraphicsProfiles.Profile.QUALITY);
    assertEquals(8, distance.value);
    assertEquals(0, h.saves);
    assertEquals(2, plan.changes.size());
    p.apply(plan);
    assertEquals(16, distance.value);
    assertEquals(Clouds.FANCY, clouds.value);
    assertEquals(GraphicsProfiles.Profile.QUALITY, p.effective());
    assertTrue(p.canUndo());
    p.undo();
    assertEquals(8, distance.value);
    assertEquals(Clouds.OFF, clouds.value);
    assertFalse(p.canUndo());
  }

  @Test
  void unsafeControlsAndUserChoicesStayUntouched() {
    Host h = new Host();
    Binding scaler = h.add("amplio:upscaler", "DLSS"),
        pack = h.add("lucerna:shaderpack", "user.zip"),
        fg = h.add("amplio:frame_generation", true),
        colors = h.add("tflcore:colors/enabled", true);
    h.add("minecraft:particles", 0);
    GraphicsProfiles p = new GraphicsProfiles(h);
    p.apply(p.prepare(GraphicsProfiles.Profile.PERFORMANCE));
    assertEquals(0, scaler.writes);
    assertEquals(0, pack.writes);
    assertEquals(0, fg.writes);
    assertEquals(0, colors.writes);
  }

  @Test
  void nativeApplyFailureRestoresEveryTouchedValueAndSavesRestoration() {
    Host h = new Host();
    Binding a = h.add("minecraft:renderDistance", 8), b = h.add("minecraft:particles", 0);
    h.failSave = true;
    GraphicsProfiles p = new GraphicsProfiles(h);
    assertThrows(
        IllegalStateException.class,
        () -> p.apply(p.prepare(GraphicsProfiles.Profile.PERFORMANCE)));
    assertEquals(8, a.value);
    assertEquals(0, b.value);
    assertEquals(2, h.saves);
    assertFalse(p.canUndo());
  }

  @Test
  void failureAfterOneMutationRollsBackThatOptionToo() {
    Host h = new Host();
    Binding a = h.add("minecraft:render_distance", 8), b = h.add("minecraft:particles", 0);
    b.fail = true;
    GraphicsProfiles p = new GraphicsProfiles(h);
    assertThrows(
        IllegalStateException.class,
        () -> p.apply(p.prepare(GraphicsProfiles.Profile.PERFORMANCE)));
    assertEquals(8, a.value);
    assertEquals(0, b.value);
    assertFalse(a.changed());
    assertFalse(b.changed());
  }

  @Test
  void changedBackendInvalidatesPreviewBeforeAnyMutation() {
    Host h = new Host();
    Binding a = h.add("minecraft:render_distance", 8), b = h.add("minecraft:particles", 0);
    GraphicsProfiles p = new GraphicsProfiles(h);
    GraphicsProfiles.Plan plan = p.prepare(GraphicsProfiles.Profile.QUALITY);
    b.value = 1;
    b.saved = 1;
    assertThrows(IllegalStateException.class, () -> p.apply(plan));
    assertEquals(0, a.writes);
    assertEquals(0, h.saves);
  }

  @Test
  void pendingUserChangesCannotBeOverwritten() {
    Host h = new Host();
    Binding a = h.add("minecraft:render_distance", 8);
    a.value = 9;
    assertThrows(
        IllegalStateException.class,
        () -> new GraphicsProfiles(h).prepare(GraphicsProfiles.Profile.QUALITY));
    assertEquals(9, a.value);
  }

  @Test
  void unavailableAndRejectedValuesAreReportedAndRemainUntouched() {
    Host h = new Host();
    Binding a = h.add("minecraft:render_distance", 8), b = h.add("minecraft:clouds", Clouds.OFF);
    a.enabled = false;
    b.reject = true;
    GraphicsProfiles.Plan plan = new GraphicsProfiles(h).prepare(GraphicsProfiles.Profile.QUALITY);
    assertEquals(2, plan.omitted.size());
    assertTrue(plan.changes.isEmpty());
    assertEquals(0, a.writes);
  }

  @Test
  void manualEditShowsCustomAndUndoCannotOverwriteIt() {
    Host h = new Host();
    Binding a = h.add("sodium:general.render_distance", 8);
    GraphicsProfiles p = new GraphicsProfiles(h);
    p.apply(p.prepare(GraphicsProfiles.Profile.QUALITY));
    a.value = 18;
    a.saved = 18;
    assertNull(p.effective());
    assertFalse(p.canUndo());
    assertNull(new GraphicsProfiles(h).effective());
  }

  @Test
  void providerConfirmationIsNeverBypassed() {
    Host h = new Host();
    Binding a = h.add("minecraft:render_distance", 8);
    h.confirmation = "Release Colores first";
    GraphicsProfiles p = new GraphicsProfiles(h);
    assertThrows(
        IllegalStateException.class, () -> p.apply(p.prepare(GraphicsProfiles.Profile.QUALITY)));
    assertEquals(8, a.value);
  }
}
