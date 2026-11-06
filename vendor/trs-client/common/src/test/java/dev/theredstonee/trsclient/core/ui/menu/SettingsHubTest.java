package dev.theredstonee.trsclient.core.ui.menu;

import static org.junit.jupiter.api.Assertions.*;
import dev.theredstonee.trsclient.core.i18n.I18n;
import dev.theredstonee.trsclient.core.ui.UiKey;
import org.junit.jupiter.api.Test;

class SettingsHubTest {
  @Test void searchOpensTheExactSettingAndBackKeepsTheQueryWithoutMutatingValues(){
    I18n.use("en");MenuRailTest.FakeHost host=new MenuRailTest.FakeHost();
    ModMenu menu=new ModMenu(host).showSettingsHub();MenuRailTest.RecordingCanvas canvas=new MenuRailTest.RecordingCanvas();
    boolean enabled=host.modules().keystrokes.isEnabled();int color=host.modules().keystrokesPressedColor.argb();
    menu.render(canvas,640,360,-100,-100);int[] field=menu.testSearchBox();assertTrue(menu.mouseClicked(field[0]+4,field[1]+4,0));
    String query=host.modules().keystrokesPressedColor.label();for(char ch:query.toCharArray())assertTrue(menu.charTyped(ch));
    canvas.clear();menu.render(canvas,640,360,-100,-100);int[] result=canvas.visible(query);assertNotNull(result);
    int[] resultBox=menu.testFirstSettingResultBox();
    assertTrue(menu.mouseClicked(resultBox[0]+5,resultBox[1]+5,0));canvas.clear();menu.render(canvas,640,360,-100,-100);
    assertNotNull(canvas.visible(query),"The requested color setting is revealed in its real detail panel");
    menu.keyPressed(0,UiKey.ESCAPE,false);canvas.clear();menu.render(canvas,640,360,-100,-100);
    assertNotNull(canvas.visible(I18n.tr("overhaul.settings")));assertEquals(query,menu.testSearchText());
    assertEquals(enabled,host.modules().keystrokes.isEnabled());assertEquals(color,host.modules().keystrokesPressedColor.argb());
  }
}
