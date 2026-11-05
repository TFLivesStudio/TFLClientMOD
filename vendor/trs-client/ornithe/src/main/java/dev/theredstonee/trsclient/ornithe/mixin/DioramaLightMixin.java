package dev.theredstonee.trsclient.ornithe.mixin;

import dev.theredstonee.trsclient.ornithe.OrnitheClient;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Vanilla and Starlight both notify this region after publishing changed client light. */
@Mixin(World.class)
public abstract class DioramaLightMixin {
    @Inject(method="notifyRegionChanged(IIIIII)V",at=@At("TAIL"))
    private void tfl$publishedLight(int minX,int minY,int minZ,int maxX,int maxY,int maxZ,CallbackInfo ci) {
        var client=OrnitheClient.get();
        if(client!=null&&client.ready&&client.mc.world==(Object)this&&client.diorama!=null)
            client.diorama.changedRegion(minX,minZ,maxX,maxZ);
    }
}
