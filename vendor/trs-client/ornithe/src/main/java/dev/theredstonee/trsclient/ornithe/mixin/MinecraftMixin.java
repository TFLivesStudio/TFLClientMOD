package dev.theredstonee.trsclient.ornithe.mixin;

import dev.theredstonee.trsclient.core.product.Product;
import dev.theredstonee.trsclient.ornithe.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.menu.TitleScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Minecraft.class)
public abstract class MinecraftMixin {
  @Inject(method="tick",at=@At("HEAD"))
  private void tfl$windowIdentity(CallbackInfo ci){
    if(!Product.safeMode())WindowIdentity.refresh();
  }

  @Inject(method="renderLoadingScreen",at=@At("HEAD"),cancellable=true)
  private void tfl$boot(net.minecraft.client.render.texture.TextureManager textures,CallbackInfo ci){
    if(!Product.safeMode()){BootScreen.draw();ci.cancel();}
  }

  @Inject(method="toggleFullscreen",at=@At("TAIL"))
  private void tfl$refreshIcon(CallbackInfo ci){if(!Product.safeMode())WindowIdentity.windowChanged();}

  @Inject(method="startGame",at=@At("HEAD"))private void tfl$worldLoad(String id,String name,net.minecraft.world.WorldSettings settings,CallbackInfo ci){var c=OrnitheClient.get();if(c!=null)c.worldLoadNanos=System.nanoTime();}

  @Inject(method = "toggleFullscreen", at = @At("HEAD"))
  private void tfl$rememberWindow(CallbackInfo ci) {
    var c = OrnitheClient.get();
    if (c != null && c.ready) c.borderless.beforeToggle();
  }

  @Redirect(
      method = "toggleFullscreen",
      at = @At(value = "INVOKE", target = "Lorg/lwjgl/opengl/Display;setFullscreen(Z)V"))
  private void tfl$fullscreen(boolean flag) throws org.lwjgl.LWJGLException {
    var c = OrnitheClient.get();
    if (c == null || !c.ready || !c.borderless.apply(flag))
      org.lwjgl.opengl.Display.setFullscreen(flag);
  }

  @Inject(
      method = {"doAttack", "doUse"},
      at = @At("HEAD"),
      cancellable = true)
  private void tfl$cameraOnly(CallbackInfo ci) {
    var c = OrnitheClient.get();
    if (c != null && c.ready && c.cinematic.detached()) ci.cancel();
  }

  @Inject(method = "getMaxFramerate", at = @At("RETURN"), cancellable = true)
  private void tfl$limit(
      org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable<Integer> cir) {
    var c = OrnitheClient.get();
    if(c!=null&&c.ready){
      int limit=cir.getReturnValue();
      if(c.mc.world==null&&c.mc.screen instanceof CoreScreen)limit=c.modules.menuFpsLimit.getInt();
      cir.setReturnValue(c.performance.limit(limit));
    }
  }

  @Redirect(
      method = "tick",
      at = @At(value = "INVOKE", target = "Lorg/lwjgl/input/Mouse;getEventDWheel()I"))
  private int tfl$wheel() {
    int amount = org.lwjgl.input.Mouse.getEventDWheel();
    var c = OrnitheClient.get();
    return c == null ? amount : c.wheel(amount);
  }

  @Inject(method = "openScreen", at = @At("TAIL"))
  private void tfl$title(Screen screen, CallbackInfo ci) {
    OrnitheClient c = OrnitheClient.get();
    Minecraft mc = (Minecraft) (Object) this;
    if(c!=null&&c.ready&&!Product.safeMode()&&mc.screen instanceof net.minecraft.client.gui.screen.menu.options.VideoOptionsScreen vanilla){mc.openScreen(new CoreScreen(new dev.theredstonee.trsclient.core.ui.graphics.GraphicsUi(new VanillaGraphics(c,vanilla)),false));return;}
    if(c!=null&&c.ready&&!Product.safeMode()&&mc.screen!=null&&OrnitheGraphics.open(c,mc.screen))return;
    if (c != null
        && c.ready
        && c.navigation != null
        && !Product.safeMode()
        && mc.screen != null
        && mc.screen.getClass() == net.minecraft.client.gui.screen.menu.GameMenuScreen.class) {
      mc.openScreen(c.navigation.pause());
      return;
    }
    if (c != null
        && c.ready
        && !Product.safeMode()
        && mc.screen != null
        && mc.screen.getClass() == TitleScreen.class) mc.openScreen(c.title());
  }

  @Inject(method = "reloadResources", at = @At("TAIL"))
  private void tfl$reload(CallbackInfo ci) {
    OrnitheClient c = OrnitheClient.get();
    if (c != null && c.ready) c.reload();
  }
}
