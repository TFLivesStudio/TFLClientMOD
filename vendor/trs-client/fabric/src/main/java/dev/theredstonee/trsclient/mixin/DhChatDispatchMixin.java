//? if >=26.3 {
/*package dev.theredstonee.trsclient.mixin;
import com.seibel.distanthorizons.common.wrappers.minecraft.MinecraftClientWrapper;
import dev.theredstonee.trsclient.compat.dh.DhShaderOrdering;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
@Mixin(value=MinecraftClientWrapper.class,remap=false)
public abstract class DhChatDispatchMixin {
    @Inject(method="sendChatMessage",at=@At("HEAD"),cancellable=true,remap=false)
    private void tfl$chatBeforeRenderPass(String message,CallbackInfo ci){DhShaderOrdering.chat(message);ci.cancel();}
}

*///?}
