//? if >=26.3 {
/*package dev.theredstonee.trsclient.mixin;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
@Pseudo
@Mixin(targets="net.caffeinemc.mods.sodium.client.gui.VideoSettingsScreen",remap=false)
public abstract class SodiumGraphicsMixin {
 @Inject(method="init",at=@At("TAIL"))
 private void tfl$graphics(CallbackInfo ci){
  try{var host=new dev.theredstonee.trsclient.screen.SodiumGraphics(this);dev.theredstonee.trsclient.compat.Mc.setScreen(new dev.theredstonee.trsclient.screen.TrsUiScreen("Gráficos",new dev.theredstonee.trsclient.core.ui.graphics.GraphicsUi(host)));}
  catch(RuntimeException e){dev.theredstonee.trsclient.TrsClient.LOGGER.warn("Unified graphics unavailable; keeping native controls",e);}
 }
}
*///?}
