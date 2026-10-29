package dev.theredstonee.trsclient.ornithe.mixin;
import dev.theredstonee.trsclient.ornithe.*;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.*;
/** Menu text uses the same atlas and measurements as every TFL screen. */
@Mixin(net.minecraft.client.render.TextRenderer.class)
public abstract class MenuFontMixin {
 @Shadow public abstract boolean getUnicode();
 @Unique private OrnitheClient tfl$client(String text){var c=OrnitheClient.get();return c==null||!c.ready||c.mc.screen==null||getUnicode()||dev.theredstonee.trsclient.core.product.Product.safeMode()||!c.canvas.supportsText(text)?null:c;}
 @Inject(method="draw(Ljava/lang/String;FFIZ)I",at=@At("HEAD"),cancellable=true)
 private void tfl$draw(String text,float x,float y,int color,boolean shadow,CallbackInfoReturnable<Integer> cir){var c=tfl$client(text);if(c==null)return;
  c.canvas.begin(c.window().getWidth(),c.window().getHeight(),c.window().getScale());try{c.canvas.push();c.canvas.translate(x,y);c.canvas.text(text,0,0,color>>>24==0?color|0xff000000:color,shadow);c.canvas.pop();cir.setReturnValue((int)x+c.canvas.textWidth(text));}finally{c.canvas.end();}
 }
 @Inject(method="getWidth(Ljava/lang/String;)I",at=@At("HEAD"),cancellable=true)
 private void tfl$width(String text,CallbackInfoReturnable<Integer> cir){var c=tfl$client(text);if(c!=null)cir.setReturnValue(c.canvas.textWidth(text));}
}
