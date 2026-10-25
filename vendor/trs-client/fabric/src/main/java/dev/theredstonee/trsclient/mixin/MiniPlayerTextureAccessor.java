//? if >=26.3 {
/*package dev.theredstonee.trsclient.mixin;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import net.minecraft.client.gui.render.pip.PictureInPictureRenderer;
import com.mojang.renderpearl.api.textures.GpuTextureView;
@Mixin(PictureInPictureRenderer.class)
public interface MiniPlayerTextureAccessor {
 @Accessor("textureView") GpuTextureView tfl$miniTexture();
 @Accessor("textureView") void tfl$miniTexture(GpuTextureView v);
 @Accessor("texture") com.mojang.renderpearl.api.textures.GpuTexture tfl$miniColor();
 @Accessor("texture") void tfl$miniColor(com.mojang.renderpearl.api.textures.GpuTexture v);
 @Accessor("depthTexture") com.mojang.renderpearl.api.textures.GpuTexture tfl$miniDepth();
 @Accessor("depthTexture") void tfl$miniDepth(com.mojang.renderpearl.api.textures.GpuTexture v);
 @Accessor("depthTextureView") GpuTextureView tfl$miniDepthView();
 @Accessor("depthTextureView") void tfl$miniDepthView(GpuTextureView v);

}
*///?}
