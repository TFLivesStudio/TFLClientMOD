package dev.theredstonee.trsclient.ornithe.mixin;

import dev.theredstonee.trsclient.ornithe.OrnitheClient;
import net.minecraft.client.gui.GameGui;
import net.minecraft.client.render.Window;
import net.minecraft.scoreboard.ScoreboardObjective;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(GameGui.class)
public abstract class VanillaHudMixin {
  @Inject(method = "renderScoreboardObjective", at = @At("HEAD"), cancellable = true)
  private void tfl$score(ScoreboardObjective objective, Window win, CallbackInfo ci) {
    var c = OrnitheClient.get();
    if (c != null && c.ready && c.capabilities.active(c.modules.qol.scoreboard)) ci.cancel();
  }

  @Inject(method = "renderBossBars", at = @At("HEAD"), cancellable = true)
  private void tfl$boss(CallbackInfo ci) {
    var c = OrnitheClient.get();
    if (c != null && c.ready && c.capabilities.active(c.modules.qol.bossBar)) {
      if (net.minecraft.client.gui.BossBar.timer > 0) net.minecraft.client.gui.BossBar.timer--;
      ci.cancel();
    }
  }

  @Redirect(
      method = "render",
      at = @At(value = "FIELD", target = "Lnet/minecraft/client/gui/GameGui;titleTime:I"))
  private int tfl$titleTime(GameGui gui) {
    var c = OrnitheClient.get();
    return c != null && c.ready && c.capabilities.active(c.modules.qol.titles)
        ? 0
        : ((GameGuiAccess) gui).tfl$titleTime();
  }

  @Inject(method = "render", at = @At("HEAD"))
  private void tfl$partial(float partial, CallbackInfo ci) {
    var c = OrnitheClient.get();
    if (c != null) c.hudPartial = partial;
  }
}
