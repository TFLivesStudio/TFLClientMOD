//? if >=26.3 {
/*package dev.theredstonee.trsclient.mixin;
import dev.theredstonee.trsclient.core.ui.*;
import dev.theredstonee.trsclient.ui.*;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.*;
// Retain Sodium's native settings controller; replace its surface drawing only.
@Pseudo
@Mixin(targets="net.caffeinemc.mods.sodium.client.gui.widgets.AbstractWidget",remap=false)
public abstract class SodiumVisualMixin {
 @Inject(method="drawRect",at=@At("HEAD"),cancellable=true,remap=false)
 private void tfl$surface(GuiGraphicsExtractor graphics,int x1,int y1,int x2,int y2,int color,CallbackInfo ci){
  if(dev.theredstonee.trsclient.core.product.Product.safeMode())return;
  int r=color>>16&255,g=color>>8&255,b=color&255,a=color>>>24;
  int themed=Math.abs(r-g)<20&&Math.abs(g-b)<20?Theme.get().surfaceHigh:(g>r*1.2&&g>b*.9?Theme.get().accent:color);
  Gfx gfx=Gfx.of(graphics);Canvas canvas=GfxCanvas.of(gfx,net.minecraft.client.Minecraft.getInstance().font);
  SelectionStyle.rounded(canvas,x1,y1,x2-x1,y2-y1,5,ColorMath.withAlpha(themed,a));ci.cancel();
 }
}
*///?}
