//? if >=26.3 {
/*package dev.theredstonee.trsclient.mixin;
import dev.theredstonee.trsclient.core.ui.menus.*;
import dev.theredstonee.trsclient.compat.Mc;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.invoke.arg.Args;
@Mixin(net.minecraft.client.gui.screens.ChatScreen.class)
public abstract class PauseChatScreenMixin {
 @ModifyArgs(method="extractRenderState",at=@At(value="INVOKE",target="Lnet/minecraft/client/gui/GuiGraphicsExtractor;fill(IIIII)V"))
 private void tfl$inputBackground(Args args){if((Object)this instanceof PauseOverlay){int width=Mc.window().getGuiScaledWidth();args.set(0,PauseLayout.chatLeft(width));args.set(2,width-10);}}
}
*///?}
