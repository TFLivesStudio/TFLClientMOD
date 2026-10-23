package dev.theredstonee.trsclient.core.ui.menu;
import dev.theredstonee.trsclient.core.module.HudModule;
import dev.theredstonee.trsclient.core.ui.Canvas;
import dev.theredstonee.trsclient.core.ui.UiKey;
import org.junit.jupiter.api.Test;
import java.lang.reflect.Proxy;
import java.util.Arrays;
import static org.junit.jupiter.api.Assertions.*;

class HudAvailabilityTest {
    @Test void unavailableRecordingPreviewIsNeitherDrawnNorKeyboardSelected() {
        MenuRailTest.FakeHost delegate=new MenuRailTest.FakeHost();
        final HudModule clips=delegate.modules().clips,fps=delegate.modules().fps;
        clips.setEnabled(true);clips.availableWhen(()->false);fps.setEnabled(true);
        MenuHost host=(MenuHost)Proxy.newProxyInstance(MenuHost.class.getClassLoader(),new Class<?>[]{MenuHost.class},(p,m,args)->
            m.getName().equals("hudItems")?Arrays.asList(item(clips,"BACKENDREC"),item(fps,"LOCALFPS")):m.invoke(delegate,args));
        HudEditor editor=new HudEditor(host);MenuRailTest.RecordingCanvas canvas=new MenuRailTest.RecordingCanvas();
        editor.render(canvas,960,540,-100,-100);
        assertNull(canvas.visible("BACKENDREC"));assertNotNull(canvas.visible("LOCALFPS"));
        double oldClip=clips.position().offsetX,oldFps=fps.position().offsetX;
        editor.keyPressed(0,UiKey.TAB,false);editor.keyPressed(0,UiKey.RIGHT,false);
        assertEquals(oldClip,clips.position().offsetX);assertNotEquals(oldFps,fps.position().offsetX);
    }
    private static HudItem item(final HudModule module,final String label) {
        return new HudItem(){public HudModule module(){return module;}public int width(){return 80;}public int height(){return 12;}public void draw(Canvas c){c.text(label,0,0,0xFFFFFFFF,false);}};
    }
}
