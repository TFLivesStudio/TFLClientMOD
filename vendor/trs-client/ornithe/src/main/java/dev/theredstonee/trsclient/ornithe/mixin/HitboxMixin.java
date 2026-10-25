package dev.theredstonee.trsclient.ornithe.mixin;

import dev.theredstonee.trsclient.ornithe.OrnitheClient;
import net.minecraft.client.render.entity.EntityRenderDispatcher;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.invoke.arg.Args;

@Mixin(EntityRenderDispatcher.class)
public abstract class HitboxMixin {
  @Shadow private boolean renderHitboxes;

  @Redirect(
      method = "render(Lnet/minecraft/entity/Entity;DDDFFZ)Z",
      at =
          @At(
              value = "FIELD",
              target =
                  "Lnet/minecraft/client/render/entity/EntityRenderDispatcher;renderHitboxes:Z"))
  private boolean tfl$boxes(EntityRenderDispatcher dispatcher) {
    var c = OrnitheClient.get();
    return renderHitboxes || (c != null && c.ready && c.capabilities.active(c.modules.hitboxes));
  }

  @ModifyArgs(
      method = "renderHitbox",
      at =
          @At(
              value = "INVOKE",
              target =
                  "Lnet/minecraft/client/render/world/WorldRenderer;renderOutlineShape(Lnet/minecraft/util/math/Box;IIII)V"))
  private void tfl$color(Args args) {
    var c = OrnitheClient.get();
    if (c == null || !c.ready || !c.capabilities.active(c.modules.hitboxes)) return;
    int rgb = c.modules.hitboxColor.rgb();
    args.set(1, rgb >> 16 & 255);
    args.set(2, rgb >> 8 & 255);
    args.set(3, rgb & 255);
  }
}
