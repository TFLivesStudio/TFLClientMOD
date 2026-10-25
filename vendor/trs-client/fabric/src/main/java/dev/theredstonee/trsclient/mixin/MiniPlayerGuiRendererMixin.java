//? if >=26.3 {
/*package dev.theredstonee.trsclient.mixin;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import net.minecraft.client.gui.render.GuiRenderer;
import net.minecraft.client.gui.render.pip.PictureInPictureRenderer;
import net.minecraft.client.renderer.state.gui.pip.PictureInPictureRenderState;
import dev.theredstonee.trsclient.hud.*;
import java.util.Map;
@Mixin(GuiRenderer.class)
public abstract class MiniPlayerGuiRendererMixin {
 @Shadow @Final private Map<Class<? extends PictureInPictureRenderState>,PictureInPictureRenderer<?>> pictureInPictureRenderers;
 @Inject(method="<init>",at=@At("TAIL"))
 private void tfl$miniRenderer(CallbackInfo ci){pictureInPictureRenderers.put(MiniPlayerFrame.class,new MiniPlayerRenderer());}
}
*///?}
