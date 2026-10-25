package dev.theredstonee.trsclient.core.ui;
import dev.theredstonee.trsclient.core.module.*;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
class GlassSettingsTest {
    @Test void keyColorsSurviveConfigRoundTripIncludingTransparency(){
        TrsModules a=new TrsModules();a.keystrokesPressedColor.setArgb(0x4055AAEE);a.keystrokesPressedText.setArgb(0xEEDDFFFF);
        TrsModules b=new TrsModules();b.keystrokes.read(a.keystrokes.write());
        assertEquals(0x4055AAEE,b.keystrokesPressedColor.argb());assertEquals(0xEEDDFFFF,b.keystrokesPressedText.argb());
    }
    @Test void cloudOptOutSurvivesConfigRoundTrip(){
        TrsModules a=new TrsModules();a.mapCloudFog.set(false);
        TrsModules b=new TrsModules();b.minimap.read(a.minimap.write());assertFalse(b.mapCloudFog.get());
    }
    @Test void glassSettingsPersistWithoutDestroyingUnrelatedSettings(){
        TrsModules a=new TrsModules();dev.theredstonee.trsclient.core.config.ModuleConfig c=a.titleScreen.write();
        c.numbers.put("panelOpacity",55d);c.numbers.put("cornerRadius",12d);c.colors.put("menuAccent","#22AAEE");
        a.titleScreen.read(c);
        assertEquals(12,UiAppearance.radius());assertEquals(0xFF22AAEE,UiAppearance.accent());
        assertEquals(140,UiAppearance.glass(0xFF102030)>>>24);
        assertEquals(55d,a.titleScreen.write().numbers.get("panelOpacity"));
    }

    @Test void pauseAndNativeWorldMenusLeaveTheFullscreenBackgroundUntouched(){
        final int[] full={0};
        Canvas canvas=(Canvas)java.lang.reflect.Proxy.newProxyInstance(Canvas.class.getClassLoader(),new Class[]{Canvas.class},(proxy,method,args)->{
            String name=method.getName();
            if(name.equals("fill")&&(int)args[0]==0&&(int)args[1]==0&&(int)args[2]==800&&(int)args[3]==600)full[0]++;
            if(name.equals("textWidth"))return ((String)args[0]).length()*6;
            if(name.equals("clip"))return args[0];
            if(name.equals("lineHeight"))return 10;
            if(name.equals("pixelScale"))return 1;
            if(method.getReturnType()==boolean.class)return false;
            return null;
        });
        dev.theredstonee.trsclient.core.ui.menus.ActionListUi pause=new dev.theredstonee.trsclient.core.ui.menus.ActionListUi("Pause",java.util.Collections.emptyList(),()->{},()->{}).transparent();
        pause.render(canvas,800,600,0,0);
        dev.theredstonee.trsclient.core.ui.menus.MenuSkin.background(canvas,800,600,true,32,578);
        assertEquals(0,full[0],"no fullscreen tint/solid background over gameplay");
    }
}
