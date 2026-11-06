package dev.theredstonee.trsclient.core.ui;

import dev.theredstonee.trsclient.core.module.*;

/** Persisted shared menu controls; no loader or GPU dependency. */
public final class UiAppearance {
    private static NumberSetting opacity, radius;
    private static ColorSetting accent;
    private static BoolSetting reducedMotion;
    private UiAppearance() {}
    public static void bind(dev.theredstonee.trsclient.core.module.Module module) {
        reducedMotion=module.add(new BoolSetting("reducedMotion","Reduce interface motion",false));
        opacity=module.add(new NumberSetting("panelOpacity", "Glass panel opacity", 82, 35, 100, 1, "", "%"));
        radius=module.add(new NumberSetting("cornerRadius", "Menu corner radius", 8, 0, 16, 1, ""));
        accent=module.add(new ColorSetting("menuAccent", "Menu accent", 0x3B82F6));
    }
    public static boolean reducedMotion(){return reducedMotion!=null && reducedMotion.get();}
    public static int radius(){return radius==null?8:radius.getInt();}
    public static int accent(){return accent==null?SelectionStyle.BLUE:accent.argb();}
    public static int glass(int color){return ColorMath.withAlpha(color, Math.round((color>>>24)*(opacity==null?.82f:opacity.getFloat()/100f)));}
}
