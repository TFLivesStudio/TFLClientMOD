package dev.theredstonee.trsclient.ornithe.mixin;

import net.minecraft.network.CompressionEncoder;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(CompressionEncoder.class)
public interface CompressionEncoderAccess {
  @Accessor("threshold")
  int tfl$threshold();
}
