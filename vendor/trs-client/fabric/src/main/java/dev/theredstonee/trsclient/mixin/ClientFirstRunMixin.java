package dev.theredstonee.trsclient.mixin;
//? if >=26.3 {
/*import net.minecraft.client.Options;
import net.minecraft.client.tutorial.TutorialSteps;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
@Mixin(Options.class)
public abstract class ClientFirstRunMixin {
    @Shadow public boolean onboardAccessibility;
    @Inject(method={"<init>","load"},at=@At("RETURN"))
    private void tfl$firstRun(CallbackInfo ci) {
        // Dismiss only the redundant introduction; preserve tutorial progress and accessibility preferences.
        onboardAccessibility=false;
    }
}
*///?} else {
public abstract class ClientFirstRunMixin { }
//?}
