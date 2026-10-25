package dev.theredstonee.trsclient.ornithe.mixin;

import dev.theredstonee.trsclient.core.emote.EmoteRig;
import dev.theredstonee.trsclient.ornithe.OrnitheClient;
import net.minecraft.client.render.model.ModelPart;
import net.minecraft.client.render.model.entity.HumanoidModel;
import net.minecraft.entity.Entity;
import net.minecraft.entity.living.player.PlayerEntity;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(HumanoidModel.class)
public abstract class HumanoidModelMixin {
  @Shadow public ModelPart head, hat, body, rightArm, leftArm, rightLeg, leftLeg;
  @Unique private final float[] tfl$rest = new float[EmoteRig.PARTS * EmoteRig.STRIDE];
  @Unique private final float[] tfl$pose = new float[EmoteRig.PARTS * EmoteRig.STRIDE];
  @Unique private boolean tfl$modified;

  @Unique
  private ModelPart tfl$part(int i) {
    return switch (i) {
      case 0 -> head;
      case 1 -> body;
      case 2 -> rightArm;
      case 3 -> leftArm;
      case 4 -> rightLeg;
      default -> leftLeg;
    };
  }

  @Unique
  private void tfl$read(float[] data) {
    for (int i = 0; i < 6; i++) {
      ModelPart p = tfl$part(i);
      int o = i * 6;
      data[o] = p.x;
      data[o + 1] = p.y;
      data[o + 2] = p.z;
      data[o + 3] = p.rotationX;
      data[o + 4] = p.rotationY;
      data[o + 5] = p.rotationZ;
    }
  }

  @Unique
  private void tfl$write(float[] data) {
    for (int i = 0; i < 6; i++) {
      ModelPart p = tfl$part(i);
      int o = i * 6;
      p.x = data[o];
      p.y = data[o + 1];
      p.z = data[o + 2];
      p.rotationX = data[o + 3];
      p.rotationY = data[o + 4];
      p.rotationZ = data[o + 5];
    }
    hat.x = head.x;
    hat.y = head.y;
    hat.z = head.z;
    hat.rotationX = head.rotationX;
    hat.rotationY = head.rotationY;
    hat.rotationZ = head.rotationZ;
  }

  @Inject(method = "setupAnimation", at = @At("HEAD"))
  private void tfl$restore(
      float swing,
      float amount,
      float age,
      float yaw,
      float pitch,
      float scale,
      Entity entity,
      CallbackInfo ci) {
    if (tfl$modified) {
      tfl$write(tfl$rest);
      tfl$modified = false;
    }
  }

  @Inject(method = "setupAnimation", at = @At("TAIL"))
  private void tfl$animate(
      float swing,
      float amount,
      float age,
      float yaw,
      float pitch,
      float scale,
      Entity entity,
      CallbackInfo ci) {
    var c = OrnitheClient.get();
    if (c == null
        || !c.ready
        || c.emotes == null
        || !(entity instanceof PlayerEntity)
        || age == 0
        || !c.emotes.controller.animating(entity.getUuid())) return;
    tfl$read(tfl$rest);
    System.arraycopy(tfl$rest, 0, tfl$pose, 0, tfl$rest.length);
    if (c.emotes.controller.apply(entity.getUuid(), System.currentTimeMillis(), tfl$pose)) {
      tfl$write(tfl$pose);
      tfl$modified = true;
      c.emotes.appliedPoses++;
    }
  }
}
