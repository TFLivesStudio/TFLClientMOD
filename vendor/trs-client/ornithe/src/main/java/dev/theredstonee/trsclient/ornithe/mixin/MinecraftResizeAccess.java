package dev.theredstonee.trsclient.ornithe.mixin;
import net.minecraft.client.Minecraft;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;
/** Allows the native integration probe to notify Minecraft after programmatic window resizing. */
@Mixin(Minecraft.class)
public interface MinecraftResizeAccess {
 @Invoker("resize")void tfl$resize(int width,int height);
}
