package dev.theredstonee.trsclient.ornithe.mixin;

import dev.theredstonee.trsclient.ornithe.OrnitheClient;
import net.minecraft.client.gui.screen.Screen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;

@Mixin(Screen.class)
public abstract class PrivacyClipboardMixin {
  @ModifyVariable(method = "setClipboard", at = @At("HEAD"), argsOnly = true, ordinal = 0)
  private static String tfl$copy(String text) {
    var c = OrnitheClient.get();
    return c != null && c.ready ? c.privacy.text(text) : text;
  }
}
