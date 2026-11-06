package dev.theredstonee.trsclient.ornithe.mixin;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
@Pseudo
@Mixin(targets="us.z1x.lucerna.gui.screen.ShaderPackScreen",remap=false)
public abstract class LucernaScreenMixin {
 @Shadow(aliases={"irisText"}) @Final @Mutable private String lucernaText;
 @Shadow private String developmentText;
 @Inject(method="<init>",at=@At("RETURN"))
 private void tfl$identity(CallbackInfo ci){if(!dev.theredstonee.trsclient.core.product.Product.safeMode()){lucernaText=dev.theredstonee.trsclient.core.product.Product.displayName("1.8.9");developmentText=null;}}
}
