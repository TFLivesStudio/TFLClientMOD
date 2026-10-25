package dev.theredstonee.trsclient.ornithe.mixin;
import dev.theredstonee.trsclient.core.hud.miniplayer.MiniPlayerRenderScope;
import net.minecraft.client.render.entity.layer.AbstractArmorLayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
@Mixin(AbstractArmorLayer.class)
public abstract class MiniPlayerArmorLayerMixin {
 @Inject(method="render",at=@At("HEAD"),cancellable=true)
 private void tfl$miniArmor(CallbackInfo ci){var settings=MiniPlayerRenderScope.current();if(settings!=null&&!settings.armor.get())ci.cancel();}
}
