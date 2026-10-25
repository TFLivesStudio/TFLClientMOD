package dev.theredstonee.trsclient.ornithe.mixin;

import dev.theredstonee.trsclient.core.connect.*;
import dev.theredstonee.trsclient.ornithe.OrnitheClient;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.menu.multiplayer.*;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(DisconnectedScreen.class)
public abstract class ReconnectMixin extends Screen {
  @Shadow private Text reason;
  @Shadow @Final private Screen parent;
  @Unique private boolean tfl$initialized;
  @Unique private ButtonWidget tfl$cancel;
  @Unique private net.minecraft.client.options.ServerListEntry tfl$server;

  @Inject(method = "init", at = @At("TAIL"))
  private void tfl$init(CallbackInfo ci) {
    var c = OrnitheClient.get();
    if (c == null || !c.ready) return;
    var m = c.modules.qol;
    if (!c.capabilities.active(m.autoReconnect) || !c.consent.approved("autoReconnect")) return;
    tfl$server = c.mc.getCurrentServerEntry();
    if (tfl$server == null) return;
    if (!tfl$initialized) {
      tfl$initialized = true;
      c.pvp.qol.reconnect.onDisconnected(
          tfl$server.ip,
          reason.getString(),
          System.currentTimeMillis(),
          (long) (m.reconnectDelay.get() * 1000),
          m.reconnectAttempts.getInt(),
          m.reconnectMode.get()
              != dev.theredstonee.trsclient.core.module.QolModules.ReconnectMode.SAFE,
          dev.theredstonee.trsclient.core.chat.ChatText.words(m.reconnectNever.get()));
    }
    tfl$cancel = new ButtonWidget(31256, width / 2 - 100, height - 42, "Cancel reconnect");
    buttons.add(tfl$cancel);
  }

  @Override
  public void tick() {
    super.tick();
    var c = OrnitheClient.get();
    if (c == null || !c.ready || tfl$cancel == null) return;
    var r = c.pvp.qol.reconnect;
    if (!c.capabilities.active(c.modules.qol.autoReconnect) || !c.consent.approved("autoReconnect"))
      r.cancel();
    tfl$cancel.message =
        r.phase() == AutoReconnect.Phase.COUNTDOWN
            ? "Cancel reconnect · " + r.secondsLeft(System.currentTimeMillis()) + "s"
            : r.phase().toString();
    if (r.due(System.currentTimeMillis()))
      c.mc.openScreen(new ConnectScreen(parent, c.mc, tfl$server));
  }

  @Inject(method = "buttonClicked", at = @At("HEAD"), cancellable = true)
  private void tfl$cancel(ButtonWidget button, CallbackInfo ci) {
    if (button == tfl$cancel) {
      OrnitheClient.get().pvp.qol.reconnect.cancel();
      ci.cancel();
    }
  }

  @Override
  public void removed() {
    super.removed();
    var c = OrnitheClient.get();
    if (c != null && c.ready) c.pvp.qol.reconnect.screenClosed();
  }
}
