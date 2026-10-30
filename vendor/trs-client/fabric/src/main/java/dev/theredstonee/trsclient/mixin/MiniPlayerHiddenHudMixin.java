package dev.theredstonee.trsclient.mixin;
//? if >=26.3 {
/*import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import net.minecraft.client.gui.Hud;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.DeltaTracker;
import dev.theredstonee.trsclient.TrsClient;
import dev.theredstonee.trsclient.compat.Mc;
import dev.theredstonee.trsclient.ui.Gfx;
@Mixin(Hud.class)
public abstract class MiniPlayerHiddenHudMixin {
 @Inject(method="extractRenderState",at=@At("HEAD"),cancellable=true)
 private void tfl$pauseHud(GuiGraphicsExtractor graphics,DeltaTracker delta,CallbackInfo ci){
  if(Mc.screen() instanceof dev.theredstonee.trsclient.core.ui.menus.PauseOverlay)ci.cancel();
 }
 @Inject(method="extractRenderState",at=@At("TAIL"))
 private void tfl$hiddenMini(GuiGraphicsExtractor graphics,DeltaTracker delta,CallbackInfo ci){
  var client=TrsClient.get();if(client!=null&&!(Mc.screen() instanceof dev.theredstonee.trsclient.core.ui.menus.PauseOverlay)&&Mc.hudHidden())client.hud().renderHiddenMini(Gfx.of(graphics));
 }
}
*///?}
