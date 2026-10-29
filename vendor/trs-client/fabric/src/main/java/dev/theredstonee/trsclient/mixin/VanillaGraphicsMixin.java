//? if >=26.3 {
/*package dev.theredstonee.trsclient.mixin;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
@Mixin(net.minecraft.client.gui.screens.options.OptionsSubScreen.class)
public abstract class VanillaGraphicsMixin {
 @Inject(method="init",at=@At("TAIL"))
 private void tfl$graphics(CallbackInfo ci){
  if((Object)this instanceof net.minecraft.client.gui.screens.options.VideoSettingsScreen screen)try{
   var host=new dev.theredstonee.trsclient.screen.VanillaGraphics(screen);dev.theredstonee.trsclient.compat.Mc.setScreen(new dev.theredstonee.trsclient.screen.TrsUiScreen("Graphics",new dev.theredstonee.trsclient.core.ui.graphics.GraphicsUi(host)));
  }catch(RuntimeException e){dev.theredstonee.trsclient.TrsClient.LOGGER.warn("Unified vanilla graphics unavailable; keeping native controls",e);}
 }
}
*///?}
