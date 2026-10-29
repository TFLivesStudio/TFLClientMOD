package dev.theredstonee.trsclient.ornithe.mixin;
import dev.theredstonee.trsclient.ornithe.*;
import dev.theredstonee.trsclient.core.ui.*;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.*;
/** Optional appearance adapter. Argentum retains all its controls, values and callbacks. */
@Pseudo
@Mixin(targets="dev.rdh.argentum.impl.gui.LegacyDrawContext",remap=false)
public abstract class ArgentumDrawMixin {
 @Unique private java.lang.reflect.Method tfl$extract;
 @Unique private String tfl$text(Object component){try{if(tfl$extract==null)for(var method:getClass().getMethods())if(method.getName().equals("extractString")){tfl$extract=method;method.setAccessible(true);break;}return (String)tfl$extract.invoke(this,component);}catch(ReflectiveOperationException e){return component.toString();}}
 @Inject(method="fill",at=@At("HEAD"),cancellable=true,remap=false)
 private void tfl$fill(int x1,int y1,int x2,int y2,int color,CallbackInfo ci){var c=OrnitheClient.get();if(c==null||!c.ready)return;
  int r=color>>16&255,g=color>>8&255,b=color&255,alpha=color>>>24;
  int themed=Math.abs(r-g)<20&&Math.abs(g-b)<20?Theme.get().surfaceHigh:(g>r*1.2&&g>b*.9?Theme.get().accent:color);
  themed=ColorMath.withAlpha(themed,alpha);
  c.canvas.begin(c.window().getWidth(),c.window().getHeight(),c.window().getScale());try{if(x2-x1>10&&y2-y1>10)SelectionStyle.rounded(c.canvas,x1,y1,x2-x1,y2-y1,5,themed);else c.canvas.fill(x1,y1,x2,y2,themed);}finally{c.canvas.end();}ci.cancel();
 }
 @Inject(method="drawString",at=@At("HEAD"),cancellable=true,remap=false)
 private void tfl$text(@Coerce Object component,int x,int y,int color,boolean shadow,CallbackInfoReturnable<Integer> cir){var c=OrnitheClient.get();if(c==null||!c.ready)return;String text=tfl$text(component);
  c.canvas.begin(c.window().getWidth(),c.window().getHeight(),c.window().getScale());try{c.canvas.text(text,x,y,color,shadow);cir.setReturnValue(x+c.canvas.textWidth(text));}finally{c.canvas.end();}
 }
 @Inject(method="getStringWidth",at=@At("HEAD"),cancellable=true,remap=false)
 private void tfl$width(@Coerce Object component,CallbackInfoReturnable<Integer> cir){var c=OrnitheClient.get();if(c!=null&&c.ready)cir.setReturnValue(c.canvas.textWidth(tfl$text(component)));}
}
