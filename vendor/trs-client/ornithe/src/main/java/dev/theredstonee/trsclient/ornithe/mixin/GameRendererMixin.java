package dev.theredstonee.trsclient.ornithe.mixin;

import dev.theredstonee.trsclient.ornithe.OrnitheClient;
import net.minecraft.client.render.GameRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.*;

@Mixin(GameRenderer.class)
public abstract class GameRendererMixin {
 @Inject(method="render(IFJ)V",at=@At(value="INVOKE",target="Lnet/minecraft/client/render/world/WorldRenderer;render(Lnet/minecraft/client/render/block/BlockLayer;DILnet/minecraft/entity/Entity;)I",ordinal=0))
 private void tfl$compareLodProjection(int pass,float partial,long limit,CallbackInfo ci){var c=OrnitheClient.get();if(c!=null&&c.ready)c.farTerrain.compareNativeProjection();}
 @Inject(method="render(IFJ)V",at=@At(value="INVOKE",target="Lnet/minecraft/client/render/platform/GlStateManager;shadeModel(I)V",ordinal=0))
 private void tfl$farTerrain(int pass,float partial,long limit,CallbackInfo ci){var c=OrnitheClient.get();if(c!=null&&c.ready)c.farTerrain.draw(partial);}

 @Inject(method="setupFog",at=@At("TAIL"))
 private void tfl$farFog(int mode,float partial,CallbackInfo ci){var c=OrnitheClient.get();if(c!=null&&c.ready)c.farTerrain.fog(mode);}
 @Inject(method="render(FJ)V",at=@At("TAIL"))
 private void tfl$hiddenMini(float partial,long limit,CallbackInfo ci){var c=OrnitheClient.get();if(c!=null)c.drawHiddenMini(partial);}

  @Inject(method = "renderWorld", at = @At("TAIL"))
  private void tfl$grade(float partial, long limit, CallbackInfo ci) {
    var c = OrnitheClient.get();
    if (c != null && c.ready) {
      c.colors.apply(partial);
      c.panorama.afterWorld();
    }
  }

  @Inject(method = "renderSnowAndRain", at = @At("HEAD"), cancellable = true)
  private void tfl$weather(float partial, CallbackInfo ci) {
    var c = OrnitheClient.get();
    if (c != null
        && c.ready
        && c.capabilities.active(c.modules.worldDetails)
        && c.modules.detailNoWeather.get()) ci.cancel();
  }

  @Redirect(
      method = "render(FJ)V",
      at =
          @At(
              value = "INVOKE",
              target =
                  "Lnet/minecraft/client/entity/living/player/LocalClientPlayerEntity;updateLocalPlayerCamera(FF)V"))
  private void tfl$look(
      net.minecraft.client.entity.living.player.LocalClientPlayerEntity player,
      float dx,
      float dy) {
    var c = OrnitheClient.get();
    if (c != null && c.ready && c.cinematic.detached()) {
      c.cinematic.controller.turn(dx, -dy);
    } else if (c != null && c.ready && c.freelook.state.active()) c.freelook.state.turn(dx, -dy);
    else player.updateLocalPlayerCamera(dx, dy);
  }

  @Redirect(
      method = "transformCamera",
      at = @At(value = "FIELD", target = "Lnet/minecraft/entity/Entity;yaw:F"))
  private float tfl$yaw(net.minecraft.entity.Entity entity) {
    var c = OrnitheClient.get();
    return c != null && c.ready && c.freelook.state.active() && entity == c.mc.player
        ? c.freelook.state.yaw()
        : entity.yaw;
  }

  @Redirect(
      method = "transformCamera",
      at = @At(value = "FIELD", target = "Lnet/minecraft/entity/Entity;lastYaw:F"))
  private float tfl$lastYaw(net.minecraft.entity.Entity entity) {
    var c = OrnitheClient.get();
    return c != null && c.ready && c.freelook.state.active() && entity == c.mc.player
        ? c.freelook.state.yaw()
        : entity.lastYaw;
  }

  @Redirect(
      method = "transformCamera",
      at = @At(value = "FIELD", target = "Lnet/minecraft/entity/Entity;pitch:F"))
  private float tfl$pitch(net.minecraft.entity.Entity entity) {
    var c = OrnitheClient.get();
    return c != null && c.ready && c.freelook.state.active() && entity == c.mc.player
        ? c.freelook.state.pitch()
        : entity.pitch;
  }

  @Redirect(
      method = "transformCamera",
      at = @At(value = "FIELD", target = "Lnet/minecraft/entity/Entity;lastPitch:F"))
  private float tfl$lastPitch(net.minecraft.entity.Entity entity) {
    var c = OrnitheClient.get();
    return c != null && c.ready && c.freelook.state.active() && entity == c.mc.player
        ? c.freelook.state.pitch()
        : entity.lastPitch;
  }

  @Inject(method = "applyHurtCam", at = @At("HEAD"), cancellable = true)
  private void tfl$hurt(float partial, CallbackInfo ci) {
    var c = OrnitheClient.get();
    if (c != null && c.ready && c.capabilities.active(c.modules.noHurtCam)) ci.cancel();
  }

  @Redirect(
      method = "updateLightMap",
      at = @At(value = "FIELD", target = "Lnet/minecraft/client/options/GameOptions;gamma:F"))
  private float tfl$gamma(net.minecraft.client.options.GameOptions options) {
    var c = OrnitheClient.get();
    return c != null && c.ready && c.capabilities.active(c.modules.fullbright)
        ? (float)
            dev.theredstonee.trsclient.core.render.Brightness.gamma(c.modules.fullbrightLevel.get())
        : options.gamma;
  }

  @Redirect(
      method = "render(FJ)V",
      at =
          @At(value = "FIELD", target = "Lnet/minecraft/client/options/GameOptions;smoothCamera:Z"))
  private boolean tfl$cinematic(net.minecraft.client.options.GameOptions options) {
    var c = OrnitheClient.get();
    return options.smoothCamera
        || (c != null && c.ready && c.zoom.isActive() && c.modules.zoomCinematic.get());
  }

  @Inject(method = "render", at = @At("HEAD"))
  private void tfl$frame(float delta, long time, CallbackInfo ci) {
    OrnitheClient c = OrnitheClient.get();
    if (c != null) c.frame();
  }

  @Inject(method = "getFov", at = @At("RETURN"), cancellable = true)
  private void tfl$fov(float delta, boolean changing, CallbackInfoReturnable<Float> cir) {
    OrnitheClient c = OrnitheClient.get();
    if (c != null && c.ready) {
      cir.setReturnValue(
          c.panorama.capture != null
              ? c.panorama.fov()
              : c.cinematic.pose != null
                  ? c.cinematic.pose.fov
                  : (float) (cir.getReturnValue() / c.zoom.factor()));
      if (changing) c.worldFov = cir.getReturnValue();
    }
  }
}
