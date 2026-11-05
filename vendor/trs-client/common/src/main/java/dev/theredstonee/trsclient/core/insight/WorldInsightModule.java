package dev.theredstonee.trsclient.core.insight;

import dev.theredstonee.trsclient.core.module.*;
import dev.theredstonee.trsclient.core.hud.*;

/** Original TFL implementation; no HWYLA code or assets. */
public final class WorldInsightModule extends HudModule {
    public final BoolSetting identifiers = add(new BoolSetting("identifiers", "Technical identifier", false));
    public final BoolSetting origin = add(new BoolSetting("origin", "Verified mod of origin", true));
    public final BoolSetting properties = add(new BoolSetting("properties", "Block properties", true));
    public final BoolSetting health = add(new BoolSetting("health", "Visible entity health", true));
    public final BoolSetting icon = add(new BoolSetting("icon", "Block icon", true));
    public final BoolSetting hideF1 = add(new BoolSetting("hideWithF1", "Hide with F1", true));
    public final BoolSetting hideMenus = add(new BoolSetting("hideInMenus", "Hide in menus", true));
    public boolean shouldDraw(boolean f1, boolean pause, boolean menu, boolean preview) {
        return preview || isEnabled() && !pause && !(f1 && hideF1.get()) && !(menu && hideMenus.get());
    }
    public WorldInsightModule() {
        super("worldInsight", "World Insight", "Information about the block or entity under the crosshair", true,
            new HudPosition(HudAnchor.TOP_CENTER, 0, 0.08));
    }
}
