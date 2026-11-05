package dev.theredstonee.trsclient.core.insight;
import dev.theredstonee.trsclient.core.ui.*;
import dev.theredstonee.trsclient.core.map.MobHeads;
/** Draws received skins, resource-pack mob faces, or native item stacks without another entity renderer. */
public final class InsightIcon {
    private InsightIcon() {}
    public static void draw(Canvas canvas, Object icon, int x, int y) {
        if (icon instanceof TextureRef) {
            TextureRef skin = (TextureRef) icon;
            Affine.image(canvas, skin, x, y, 16, 16, 8, 8, 8, 8, 0xffffffff);
            Affine.image(canvas, skin, x, y, 16, 16, 40, 8, 8, 8, 0xffffffff);
        } else if (icon instanceof MobHeads.Head) {
            MobHeads.Head head = (MobHeads.Head) icon;
            Affine.image(canvas, head.texture, x, y, 16, 16, head.u, head.v, head.w, head.h, 0xffffffff);
            if (head.hatU >= 0) Affine.image(canvas, head.texture, x, y, 16, 16, head.hatU, head.hatV, head.w, head.h, 0xffffffff);
        } else canvas.item(icon, x, y);
    }
}
