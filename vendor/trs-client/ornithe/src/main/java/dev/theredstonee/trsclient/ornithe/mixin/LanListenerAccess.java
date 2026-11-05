package dev.theredstonee.trsclient.ornithe.mixin;

import io.netty.channel.ChannelFuture;
import java.util.List;
import net.minecraft.server.network.ConnectionListener;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/** Actual bound LAN port, including worlds published through Minecraft's own LAN screen. */
@Mixin(ConnectionListener.class)
public interface LanListenerAccess {
    @Accessor("channels") List<ChannelFuture> tfl$lanChannels();
}
