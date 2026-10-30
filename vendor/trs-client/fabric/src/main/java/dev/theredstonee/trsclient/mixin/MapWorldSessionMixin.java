//? if >=26.3 {
/*package dev.theredstonee.trsclient.mixin;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
@Mixin(net.minecraft.client.multiplayer.ClientPacketListener.class)
public abstract class MapWorldSessionMixin {
 @Inject(method="handleRespawn",at=@At("TAIL"))
 private void tfl$worldBoundary(net.minecraft.network.protocol.game.ClientboundRespawnPacket packet,CallbackInfo ci){dev.diorama.WorldKey.newLevelBoundary();}
}
*///?}
