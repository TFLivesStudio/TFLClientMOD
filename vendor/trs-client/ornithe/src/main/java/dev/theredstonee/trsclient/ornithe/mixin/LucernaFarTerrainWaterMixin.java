package dev.theredstonee.trsclient.ornithe.mixin;

import dev.theredstonee.trsclient.ornithe.OrnitheClient;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** After deferred, before the caller enters the vanilla/Argentum water pass. */
@Pseudo
@Mixin(targets="us.z1x.lucerna.pipeline.DeferredWorldRenderingPipeline",remap=false)
public abstract class LucernaFarTerrainWaterMixin {
    @Inject(method="beginTranslucents",at=@At("TAIL"),require=0)
    private void tfl$water(CallbackInfo ci){
        var c=OrnitheClient.get();if(c!=null&&c.ready)c.farTerrain.drawShaderWater();
    }
}
