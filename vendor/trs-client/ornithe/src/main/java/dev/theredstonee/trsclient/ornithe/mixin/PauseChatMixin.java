package dev.theredstonee.trsclient.ornithe.mixin;
import dev.theredstonee.trsclient.ornithe.OrnitheClient;
import dev.theredstonee.trsclient.core.ui.menus.PauseOverlay;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
@Mixin(net.minecraft.client.gui.chat.ChatGui.class)
public abstract class PauseChatMixin {
  @Shadow public abstract float getChatScale();
  @Inject(method="getVisibleLineCount",at=@At("RETURN"),cancellable=true)
  private void tfl$pauseLines(CallbackInfoReturnable<Integer> result){
    var c=OrnitheClient.get();
    if(c!=null&&c.mc.screen instanceof PauseOverlay)
      result.setReturnValue(Math.min(result.getReturnValue(),Math.max(1,(int)(c.window().getHeight()*.32/(9*Math.max(.1,getChatScale()))))));
  }
}
