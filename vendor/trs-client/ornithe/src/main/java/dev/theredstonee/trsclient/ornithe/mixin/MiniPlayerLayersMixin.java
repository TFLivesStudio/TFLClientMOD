package dev.theredstonee.trsclient.ornithe.mixin;
import dev.theredstonee.trsclient.core.hud.miniplayer.MiniPlayerRenderScope;
import net.minecraft.client.render.entity.layer.AbstractArmorLayer;
import net.minecraft.client.render.entity.layer.ItemInHandLayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
@Mixin({AbstractArmorLayer.class,ItemInHandLayer.class})
public abstract class MiniPlayerLayersMixin {
    @Inject(method="render",at=@At("HEAD"),cancellable=true)
    private void tfl$miniLayers(CallbackInfo ci){var m=MiniPlayerRenderScope.current();if(m!=null&&(getClass().getName().contains("Armor")?!m.armor.get():!m.heldItems.get()))ci.cancel();}
}
