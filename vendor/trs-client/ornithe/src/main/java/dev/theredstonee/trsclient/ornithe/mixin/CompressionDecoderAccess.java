package dev.theredstonee.trsclient.ornithe.mixin;

import net.minecraft.network.CompressionDecoder;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(CompressionDecoder.class)
public interface CompressionDecoderAccess {
  @Accessor("threshold")
  int tfl$threshold();
}
