package dev.theredstonee.trsclient.mixin;
//? if >=26.3 {
/*import net.minecraft.client.gui.components.AbstractSliderButton;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
@Mixin(AbstractSliderButton.class)
public abstract class ClientSliderMixin {
    @Shadow protected double value;
    @Inject(method="extractWidgetRenderState",at=@At("HEAD"),cancellable=true)
    private void tfl$slider(GuiGraphicsExtractor g,int mouseX,int mouseY,float dt,CallbackInfo ci) {
        if(!dev.theredstonee.trsclient.menus.VanillaMenus.styled(dev.theredstonee.trsclient.compat.Mc.screen()))return;
        AbstractWidget b=(AbstractWidget)(Object)this;
        dev.theredstonee.trsclient.core.ui.Canvas c=dev.theredstonee.trsclient.ui.GfxCanvas.of(dev.theredstonee.trsclient.ui.Gfx.of(g),net.minecraft.client.Minecraft.getInstance().font);
        int x=b.getX(),y=b.getY(),w=b.getWidth(),h=b.getHeight();
        dev.theredstonee.trsclient.core.ui.SelectionStyle.button(c,x,y,w,h,false,b.isHoveredOrFocused());
        String label=c.clip(b.getMessage().getString(),Math.max(0,w-16));
        c.text(label,x+(w-c.textWidth(label))/2,y+3,b.active?0xFFF1F5FC:0xFF8090A4,false);
        dev.theredstonee.trsclient.core.ui.Redstone.slider(c,x+8,y+h-7,w-16,6,(float)value,b.isHoveredOrFocused());
        ci.cancel();
    }
}
*///?} else {
public abstract class ClientSliderMixin { }
//?}
