//? if >=26.3 {
/*package dev.theredstonee.trsclient.mixin;
import dev.theredstonee.trsclient.compat.Mc;
import dev.theredstonee.trsclient.core.ui.menus.PauseOverlay;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
@Mixin(net.minecraft.client.gui.components.ChatComponent.class)
public abstract class PauseChatMixin {
  @Inject(method="getLinesPerPage",at=@At("RETURN"),cancellable=true)
  private void tfl$pauseLines(CallbackInfoReturnable<Integer> result){
    if(Mc.screen() instanceof PauseOverlay)
      result.setReturnValue(Math.min(result.getReturnValue(),Math.max(1,(int)(Mc.window().getGuiScaledHeight()*.32/(9*Math.max(.1,Mc.chatScale()))))));
  }
}
*///?}
