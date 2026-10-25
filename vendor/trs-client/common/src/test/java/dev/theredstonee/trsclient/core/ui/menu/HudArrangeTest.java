package dev.theredstonee.trsclient.core.ui.menu;

import static org.junit.jupiter.api.Assertions.*;

import dev.theredstonee.trsclient.core.hud.*;
import dev.theredstonee.trsclient.core.i18n.I18n;
import dev.theredstonee.trsclient.core.module.HudModule;
import dev.theredstonee.trsclient.core.ui.*;
import java.lang.reflect.Proxy;
import java.util.*;
import org.junit.jupiter.api.Test;

class HudArrangeTest {
  @Test
  void explicitArrangementSeparatesWidgetsInSmallWindowsAndCanBeUndone() {
    I18n.use("en");
    for (int[] size : new int[][] {{320, 200}, {427, 240}}) {
      var delegate = new MenuRailTest.FakeHost();
      var modules = delegate.modules();
      List<HudItem> items = new ArrayList<HudItem>();
      for (HudModule module : Arrays.asList(modules.fps, modules.cps, modules.coords)) {
        module.setEnabled(true);
        module.position().set(new HudPosition(HudAnchor.TOP_LEFT, 0, 0));
        items.add(item(module));
      }
      MenuHost host =
          (MenuHost)
              Proxy.newProxyInstance(
                  MenuHost.class.getClassLoader(),
                  new Class<?>[] {MenuHost.class},
                  (proxy, method, args) ->
                      method.getName().equals("hudItems") ? items : method.invoke(delegate, args));
      HudEditor editor = new HudEditor(host);
      var canvas = new MenuRailTest.RecordingCanvas();
      editor.render(canvas, size[0], size[1], -100, -100);
      int[] button = canvas.visible("Arrange");
      assertNotNull(button);
      assertTrue(editor.mouseClicked(button[0] + 2, button[1] + 2, 0));
      List<int[]> bounds = new ArrayList<int[]>();
      for (HudItem item : items) {
        int x = HudLayout.resolveX(item.module().position(), 80, size[0]),
            y = HudLayout.resolveY(item.module().position(), 18, size[1]);
        assertTrue(x >= 0 && x + 80 <= size[0] && y >= 0 && y + 18 <= size[1]);
        bounds.add(new int[] {x, y, 80, 18});
      }
      for (int i = 0; i < bounds.size(); i++)
        for (int j = 0; j < i; j++) assertFalse(HudLayout.overlaps(bounds.get(i), bounds.get(j)));
      canvas.clear();
      editor.render(canvas, size[0], size[1], -100, -100);
      int[] undo = canvas.visible(I18n.trOr("editor.undo", "Undo"));
      assertNotNull(undo);
      editor.mouseClicked(undo[0] + 2, undo[1] + 2, 0);
      for (HudItem item : items) {
        assertEquals(0, item.module().position().offsetX);
        assertEquals(0, item.module().position().offsetY);
      }
    }
  }

  private static HudItem item(final HudModule module) {
    return new HudItem() {
      public HudModule module() {
        return module;
      }

      public int width() {
        return 80;
      }

      public int height() {
        return 18;
      }

      public void draw(Canvas c) {
        c.text(module.id(), 0, 0, 0xFFFFFFFF, false);
      }
    };
  }
}
