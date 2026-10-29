package dev.theredstonee.trsclient.ornithe.mixin;
import dev.theredstonee.trsclient.ornithe.OrnitheClient;
import dev.theredstonee.trsclient.core.ui.*;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
/** Only presentation changes: shader-pack controls keep their original input and callbacks. */
@Pseudo
@Mixin(targets="us.z1x.lucerna.gui.GuiUtil",remap=false)
public abstract class LucernaControlsMixin {
 @Inject(method="drawButton",at=@At("HEAD"),cancellable=true)
 private static void tfl$button(int x,int y,int width,int height,boolean hover,boolean disabled,CallbackInfo ci){var c=OrnitheClient.get();if(!usable(c))return;c.canvas.begin(c.mc.screen.width,c.mc.screen.height,c.window().getScale());try{SelectionStyle.button(c.canvas,x,y,width,height,false,!disabled&&hover);}finally{c.canvas.end();}ci.cancel();}
 @Inject(method="drawPanel",at=@At("HEAD"),cancellable=true)
 private static void tfl$panel(int x,int y,int width,int height,CallbackInfo ci){var c=OrnitheClient.get();if(!usable(c))return;c.canvas.begin(c.mc.screen.width,c.mc.screen.height,c.window().getScale());try{SelectionStyle.panel(c.canvas,x,y,width,height,Theme.get().surface,Theme.get().border);}finally{c.canvas.end();}ci.cancel();}
 @Unique private static boolean usable(OrnitheClient c){return c!=null&&c.ready&&c.mc.screen!=null&&!dev.theredstonee.trsclient.core.product.Product.safeMode();}
}
