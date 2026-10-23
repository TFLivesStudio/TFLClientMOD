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
    @Shadow public TutorialSteps tutorialStep;
    @Inject(method={"<init>","load"},at=@At("RETURN"))
    private void tfl$firstRun(CallbackInfo ci) {
        // Only the redundant introduction and basic tutorial; narrator and all actual accessibility settings stay untouched.
        onboardAccessibility=false;
        tutorialStep=TutorialSteps.NONE;
    }
}
*///?} else {
public abstract class ClientFirstRunMixin { }
//?}
