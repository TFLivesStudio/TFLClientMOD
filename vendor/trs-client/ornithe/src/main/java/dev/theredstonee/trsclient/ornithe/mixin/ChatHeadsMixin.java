package dev.theredstonee.trsclient.ornithe.mixin;

import dev.theredstonee.trsclient.ornithe.OrnitheClient;
import net.minecraft.client.gui.chat.ChatGui;
import net.minecraft.client.render.TextRenderer;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;

@Mixin(ChatGui.class)
public abstract class ChatHeadsMixin {
  @Unique private Text tfl$current;

  @Redirect(
      method = "addMessage(Lnet/minecraft/text/Text;IIZ)V",
      at =
          @At(
              value = "INVOKE",
              target =
                  "Lnet/minecraft/client/render/TextRenderUtils;wrapText(Lnet/minecraft/text/Text;ILnet/minecraft/client/render/TextRenderer;ZZ)Ljava/util/List;"))
  private java.util.List<Text> tfl$wrap(
      Text text, int width, TextRenderer font, boolean colors, boolean force) {
    var c = OrnitheClient.get();
    return c != null && c.ready
        ? c.heads.wrap(text, width, font, colors, force)
        : net.minecraft.client.render.TextRenderUtils.wrapText(text, width, font, colors, force);
  }

  @Redirect(
      method = "render",
      at =
          @At(
              value = "INVOKE",
              target = "Lnet/minecraft/text/Text;getFormattedString()Ljava/lang/String;"))
  private String tfl$line(Text text) {
    tfl$current = text;
    return text.getFormattedString();
  }

  @Redirect(
      method = "render",
      at =
          @At(
              value = "INVOKE",
              target =
                  "Lnet/minecraft/client/render/TextRenderer;drawWithShadow(Ljava/lang/String;FFI)I"))
  private int tfl$head(TextRenderer font, String text, float x, float y, int color) {
    var c = OrnitheClient.get();
    if (c != null && c.ready) c.heads.draw(tfl$current, x, y, color);
    return font.drawWithShadow(text, x, y, color);
  }
}
