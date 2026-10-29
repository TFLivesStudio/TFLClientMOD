package dev.theredstonee.trsclient.ornithe.mixin;
import dev.theredstonee.trsclient.ornithe.OrnitheClient;
import net.minecraft.world.chunk.WorldChunk;
import net.minecraft.world.World;
import net.minecraft.block.state.BlockState;
import net.minecraft.util.math.BlockPos;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
@Mixin(WorldChunk.class)
public abstract class DioramaChunkMixin {
 @Shadow @Final private World world;
 @Shadow @Final public int chunkX,chunkZ;
 @Inject(method="setBlockState",at=@At("RETURN"))
 private void tfl$changed(BlockPos pos,BlockState state,CallbackInfoReturnable<BlockState> cir){var c=OrnitheClient.get();if(c!=null&&c.ready&&c.mc.world==world&&c.diorama!=null)c.diorama.changed(chunkX,chunkZ);}
}
