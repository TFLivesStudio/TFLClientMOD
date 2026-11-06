package dev.theredstonee.trsclient.core.ui;

import static org.junit.jupiter.api.Assertions.*;

import java.util.*;
import org.junit.jupiter.api.Test;

class VisualComponentsTest {
  static class Draw implements Canvas {
    final List<int[]> textPositions = new ArrayList<>();

    public void fill(int a, int b, int c, int d, int color) {}

    public void text(String s, int x, int y, int c, boolean sh) {
      textPositions.add(new int[] {x, y});
    }

    public int textWidth(String s) {
      return s.length() * 6;
    }

    public int lineHeight() {
      return 9;
    }

    public String clip(String s, int n) {
      return s.substring(0, Math.min(s.length(), Math.max(0, n / 6)));
    }

    public void flush() {}

    public void scissor(int a, int b, int c, int d) {}

    public void noScissor() {}

    public void raise(float z) {}

    public void push() {}

    public void pop() {}

    public void translate(float x, float y) {}

    public void scale(float s) {}
  }

  @Test
  void fontScopeRestoresAfterAnExceptionEvenThroughAFadeWrapper() {
    final boolean[] active={false};
    Draw canvas=new Draw(){public boolean uiText(boolean enabled){boolean previous=active[0];active[0]=enabled;return previous;}};
    UiScreen broken=new UiScreen(){
      protected void draw(Canvas c,int w,int h,int mx,int my,float dt){assertTrue(active[0]);throw new IllegalStateException("test draw");}
      protected void onClosed(){}
    };
    assertThrows(IllegalStateException.class,()->broken.render(FadeCanvas.of(canvas,.5f),320,200,0,0));
    assertFalse(active[0],"Native chat/inventory drawing must see the prior canvas state");
  }

  @Test
  void paragraphBreaksCannotReachANativeFontAsMissingGlyphs() {
    List<String> lines = Paint.wrap(new Draw(), "Description\r\nNative controls\n\nHelp", 200);
    assertEquals(Arrays.asList("Description", "Native controls", "", "Help"), lines);
    for (String line : lines) {
      assertFalse(line.contains("\n"));
      assertFalse(line.contains("\r"));
    }
  }

  @Test
  void keyboardFocusVisitsOnlyVisibleActionsAndCanActivateWithoutAMouse() {
    Hits hits = new Hits();
    int[] actions = {0};
    hits.clip(10, 10, 100, 30);
    hits.add(0, 0, 4, 4, () -> fail("clipped action"));
    hits.add(10, 10, 20, 20, () -> actions[0]++);
    hits.addDrag(40, 10, 20, 20, (x, y) -> fail("a drag is not a button"));
    hits.add(60, 10, 20, 20, 1, () -> fail("right click action"));
    hits.noClip();
    assertTrue(hits.focusNext(false));
    assertTrue(hits.activateFocused());
    assertEquals(1, actions[0]);
    assertTrue(hits.focusNext(true));
    assertTrue(hits.activateFocused());
    assertEquals(2, actions[0]);
  }

  @Test
  void toolbarWrapsUsingTheSameGeometryAsItsHitRegions() {
    Draw c = new Draw();
    Hits hits = new Hits();
    int[] actions = {0};
    Toolbar toolbar = new Toolbar(c, hits, 8, 12, 90, -1, -1);
    toolbar.action("Center", () -> actions[0]++);
    toolbar.action("Rotate", () -> actions[0] += 10);
    assertEquals(64, toolbar.bottom());
    assertTrue(hits.click(10, 14, 0));
    assertEquals(1, actions[0]);
    assertTrue(hits.click(10, 42, 0));
    assertEquals(11, actions[0]);
    assertFalse(hits.click(99, 42, 0));
  }
}
