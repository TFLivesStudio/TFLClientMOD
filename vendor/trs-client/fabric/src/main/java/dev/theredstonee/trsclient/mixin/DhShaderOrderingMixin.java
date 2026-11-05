//? if >=26.3 {
/*package dev.theredstonee.trsclient.mixin;
import com.seibel.distanthorizons.core.api.internal.ClientApi;
import dev.theredstonee.trsclient.compat.dh.DhShaderOrdering;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
@Mixin(value=ClientApi.class,remap=false)
public abstract class DhShaderOrderingMixin {
    @Inject(method="renderLods",at=@At("HEAD"),remap=false)
    private void tfl$planShaderPass(CallbackInfo ci){DhShaderOrdering.beforePass();}
}

*///?}
