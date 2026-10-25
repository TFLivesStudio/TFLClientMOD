package dev.theredstonee.trsclient.ornithe.mixin;

import net.minecraft.network.Connection;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(Connection.class)
public interface ConnectionAccess {
  @Accessor("channel")
  io.netty.channel.Channel tfl$channel();
}
