package dev.theredstonee.trsclient.mixin;
//? if >=26.3 {
/*import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import net.minecraft.client.tutorial.*;
@Mixin(OpenInventoryTutorialStep.class)
public abstract class InventoryTutorialMixin {
 @Shadow @Final private Tutorial tutorial;
 @Inject(method="tick",at=@At("HEAD"),cancellable=true)
 private void tfl$skipInventoryHint(CallbackInfo ci){
  if(!dev.theredstonee.trsclient.core.product.Product.safeMode()){tutorial.setStep(TutorialSteps.CRAFT_PLANKS);ci.cancel();}
 }
}
*///?}
