package dev.theredstonee.trsclient.ornithe.mixin;

import dev.theredstonee.trsclient.core.connect.FastConnect;
import dev.theredstonee.trsclient.ornithe.OrnitheClient;
import net.minecraft.client.gui.screen.menu.multiplayer.MultiplayerScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(MultiplayerScreen.class)
public abstract class FastConnectSelectionMixin {
  @Inject(method = "moveToServer", at = @At("TAIL"))
  private void tfl$prefetch(int index, CallbackInfo ci) {
    var c = OrnitheClient.get();
    if (c == null
        || !c.ready
        || !c.capabilities.active(c.modules.fastConnect)
        || !c.modules.fastConnectPreResolve.get()) return;
    var list = ((MultiplayerScreen) (Object) this).getServerList();
    if (index >= 0 && index < list.size()) FastConnect.prefetch(list.get(index).ip);
  }
}
