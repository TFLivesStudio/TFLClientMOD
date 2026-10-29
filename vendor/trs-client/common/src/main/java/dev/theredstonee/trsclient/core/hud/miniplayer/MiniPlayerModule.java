package dev.theredstonee.trsclient.core.hud.miniplayer;
import dev.theredstonee.trsclient.core.hud.*;
import dev.theredstonee.trsclient.core.module.*;

/** Shared official Paper Doll / Mini Player module; both renderers consume these exact settings. */
public final class MiniPlayerModule extends HudModule {
    public enum Visibility implements ChoiceSetting.Option { ALWAYS("Always"),MOVEMENT("Movement"),OFF("Off");private final String label;Visibility(String label){this.label=label;}public String label(){return label;}}
    public enum Angle implements ChoiceSetting.Option { FRONT("Front"),SIDE("Side"),THREE_QUARTERS("Three quarters"),MANUAL("Manual");private final String label;Angle(String label){this.label=label;}public String label(){return label;}}
    public final ChoiceSetting<Visibility> visibility;
    public final ChoiceSetting<Angle> angle;
    public final NumberSetting opacity,rotation,fps;
    public final BoolSetting smoothing,armor,heldItems,cape,hideF1,hideMenus;
    public MiniPlayerModule(){
        super("miniPlayer","Paper Doll · Mini Player","Live 3D view of your own player, equipment and animations",true,new HudPosition(HudAnchor.TOP_LEFT,.25,.01),false);
        icon("user").category(Category.HUD);
        visibility=add(new ChoiceSetting<>("visibility","Visibility",Visibility.class,Visibility.ALWAYS));
        angle=add(new ChoiceSetting<>("cameraAngle","Camera angle",Angle.class,Angle.FRONT));
        opacity=add(new NumberSetting("opacity","Model opacity",100,0,100,5,"","%"));
        rotation=add(new NumberSetting("rotation","Manual rotation",0,-180,180,5,"","°"));
        fps=add(new NumberSetting("fpsLimit","Widget FPS limit (0 = unlimited)",60,0,240,5,""));
        smoothing=add(new BoolSetting("smoothing","Animation smoothing",true));
        armor=add(new BoolSetting("showArmor","Show armor",true));heldItems=add(new BoolSetting("showHeldItems","Show held items",true));cape=add(new BoolSetting("showCape","Show authorized cape",true));
        hideF1=add(new BoolSetting("hideWithF1","Hide with F1",true));hideMenus=add(new BoolSetting("hideInMenus","Hide in menus",true));
    }
    public float cameraYaw(){switch(angle.get()){case SIDE:return 90;case THREE_QUARTERS:return 35;case MANUAL:return rotation.getFloat();default:return 0;}}
    public boolean shouldDraw(boolean preview,boolean playerPresent,boolean moving,boolean f1,boolean menu,boolean onScreen){
        return (preview||isEnabled())&&playerPresent&&onScreen&&opacity.get()>0&&(preview||visibility.get()!=Visibility.OFF)&&
            (preview||!hideF1.get()||!f1)&&(preview||!hideMenus.get()||!menu)&&(preview||visibility.get()!=Visibility.MOVEMENT||moving);
    }
}
