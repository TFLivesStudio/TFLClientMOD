//? if >=26.3 {
/*package dev.theredstonee.trsclient.mixin;

import com.seibel.distanthorizons.common.wrappers.world.ClientLevelWrapper;
import com.seibel.distanthorizons.core.wrapperInterfaces.world.IClientLevelWrapper;
import dev.theredstonee.trsclient.compat.dh.DhWorldSessions;
import net.minecraft.client.multiplayer.ClientLevel;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value=ClientLevelWrapper.class,remap=false)
public abstract class DhClientWrapperMixin {
    @Inject(method="getWrapper(Lnet/minecraft/client/multiplayer/ClientLevel;Z)Lcom/seibel/distanthorizons/core/wrapperInterfaces/world/IClientLevelWrapper;",at=@At("RETURN"),cancellable=true,remap=false)
    private static void tfl$worldIdentity(ClientLevel level,boolean bypass,CallbackInfoReturnable<IClientLevelWrapper> result){
        if(!bypass)result.setReturnValue(DhWorldSessions.wrap(level,result.getReturnValue()));
    }
    @Inject(method="getWrapperIfDifferent",at=@At("RETURN"),cancellable=true,remap=false)
    private static void tfl$manualWorldSelection(IClientLevelWrapper previous,ClientLevel level,CallbackInfoReturnable<IClientLevelWrapper> result){
        result.setReturnValue(DhWorldSessions.wrap(level,result.getReturnValue()));
    }
}
*///?}
