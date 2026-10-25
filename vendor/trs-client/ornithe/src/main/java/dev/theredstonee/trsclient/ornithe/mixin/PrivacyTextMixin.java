package dev.theredstonee.trsclient.ornithe.mixin;

import dev.theredstonee.trsclient.ornithe.OrnitheClient;
import net.minecraft.client.render.TextRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;

@Mixin(TextRenderer.class)
public abstract class PrivacyTextMixin {
  @ModifyVariable(
      method = {"draw(Ljava/lang/String;FFIZ)I", "getWidth(Ljava/lang/String;)I"},
      at = @At("HEAD"),
      argsOnly = true,
      ordinal = 0)
  private String tfl$private(String text) {
    var c = OrnitheClient.get();
    return c != null && c.ready ? c.privacy.text(text) : text;
  }
}
