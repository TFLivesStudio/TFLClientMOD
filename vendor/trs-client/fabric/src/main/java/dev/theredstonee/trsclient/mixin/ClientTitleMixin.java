package dev.theredstonee.trsclient.mixin;
//? if >=26.3 {
/*import net.minecraft.client.Minecraft;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
@Mixin(Minecraft.class)
public abstract class ClientTitleMixin {
    @Inject(method="createTitle",at=@At("RETURN"),cancellable=true)
    private void tfl$title(CallbackInfoReturnable<String> ci) {ci.setReturnValue("TFL Client · "+ci.getReturnValue());}
}
*///?} else {
public abstract class ClientTitleMixin { }
//?}
