//? if >=26.3 {
/*package dev.theredstonee.trsclient.mixin;
import dev.theredstonee.trsclient.compat.Mc;
import dev.theredstonee.trsclient.core.ui.menus.*;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
@Mixin(net.minecraft.client.gui.components.ChatComponent.class)
public abstract class PauseChatMixin {
  @Unique private boolean tfl$pause(){return Mc.screen() instanceof PauseOverlay p&&p.pauseChatLayout();}
  @Inject(method="getWidth()I",at=@At("RETURN"),cancellable=true)
  private void tfl$width(CallbackInfoReturnable<Integer> result){if(tfl$pause())result.setReturnValue(Math.min(result.getReturnValue(),PauseLayout.chatWidth(Mc.window().getGuiScaledWidth())));}
  @ModifyArg(method="extractRenderState(Lnet/minecraft/client/gui/components/ChatComponent$ChatGraphicsAccess;IILnet/minecraft/client/gui/components/ChatComponent$DisplayMode;)V",at=@At(value="INVOKE",target="Lnet/minecraft/client/gui/components/ChatComponent$ChatGraphicsAccess;updatePose(Ljava/util/function/Consumer;)V"),index=0)
  private java.util.function.Consumer<org.joml.Matrix3x2f> tfl$origin(java.util.function.Consumer<org.joml.Matrix3x2f> original){
    if(!tfl$pause())return original;float offset=(float)(PauseLayout.chatLeft(Mc.window().getGuiScaledWidth())-4*Mc.chatScale());return pose->{pose.translate(offset,0);original.accept(pose);};
  }
  @Inject(method="getLinesPerPage",at=@At("RETURN"),cancellable=true)
  private void tfl$pauseLines(CallbackInfoReturnable<Integer> result){if(tfl$pause())result.setReturnValue(Math.min(result.getReturnValue(),Math.max(1,(int)(Mc.window().getGuiScaledHeight()*.32/(9*Math.max(.1,Mc.chatScale()))))));}
}
*///?}
