package dev.theredstonee.trsclient.ornithe.mixin;

import dev.theredstonee.trsclient.core.perf.ParticleGate;
import dev.theredstonee.trsclient.ornithe.OrnitheClient;
import net.minecraft.client.ParticleManager;
import net.minecraft.client.entity.particle.Particle;
import net.minecraft.entity.particle.ParticleType;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.*;

@Mixin(ParticleManager.class)
public abstract class ParticleManagerMixin {
  @Shadow private java.util.List<Particle>[][] particles;
  @Unique private final ParticleGate tfl$gate = new ParticleGate();

  @Inject(method = "addParticle", at = @At("HEAD"), cancellable = true)
  private void tfl$particle(
      int id,
      double x,
      double y,
      double z,
      double dx,
      double dy,
      double dz,
      int[] args,
      CallbackInfoReturnable<Particle> cir) {
    var c = OrnitheClient.get();
    if (c == null || !c.ready || !c.capabilities.active(c.modules.particles)) return;
    var type = ParticleType.byId(id);
    ParticleGate.Kind kind =
        type == ParticleType.EXPLOSION_NORMAL
                || type == ParticleType.EXPLOSION_LARGE
                || type == ParticleType.EXPLOSION_HUGE
            ? ParticleGate.Kind.EXPLOSION
            : type == ParticleType.SMOKE_NORMAL || type == ParticleType.SMOKE_LARGE
                ? ParticleGate.Kind.SMOKE
                : type == ParticleType.WATER_DROP
                    ? ParticleGate.Kind.RAIN
                    : ParticleGate.Kind.OTHER;
    int count = 0;
    for (var layer : particles) for (var list : layer) count += list.size();
    boolean hide =
        switch (kind) {
          case EXPLOSION -> c.modules.particleNoExplosions.get();
          case RAIN -> c.modules.particleNoRain.get();
          case SMOKE -> c.modules.particleNoSmoke.get();
          default -> false;
        };
    if (!tfl$gate.allow(
        kind, count, c.modules.particleLimit.getInt(), c.modules.particleAmount.get() / 100, hide))
      cir.setReturnValue(null);
  }

  @Inject(method = "setWorld", at = @At("HEAD"))
  private void tfl$reset(net.minecraft.world.World world, CallbackInfo ci) {
    tfl$gate.reset();
  }
}
