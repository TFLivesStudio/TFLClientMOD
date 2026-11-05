package dev.theredstonee.trsclient.ornithe.mixin;
import net.minecraft.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;
@Mixin(Entity.class)
public interface InsightEntityAccess {
    @Invoker("getTypeId") String tfl$insightTypeId();
}
