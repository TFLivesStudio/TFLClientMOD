package dev.theredstonee.trsclient.mixin;
//? if >=26.3 {
/*import net.minecraft.client.gui.Font;
import net.minecraft.network.chat.FontDescription;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

// Menu-local default font replacement; explicit pack fonts and Unicode accessibility remain intact.
@Mixin(Font.class)
public abstract class ClientMenuFontMixin {
    private static final FontDescription TFL_MENU_FONT=new FontDescription.Resource(dev.theredstonee.trsclient.TrsClient.id("ui"));
    @ModifyVariable(method="getGlyphSource",at=@At("HEAD"),argsOnly=true,ordinal=0)
    private FontDescription tfl$menuFont(FontDescription original) {
        if(!FontDescription.DEFAULT.equals(original))return original;
        net.minecraft.client.Minecraft mc=net.minecraft.client.Minecraft.getInstance();
        if(mc==null||mc.options==null||mc.options.forceUnicodeFont().get())return original;
        var screen=dev.theredstonee.trsclient.compat.Mc.screen();
        boolean custom=screen instanceof dev.theredstonee.trsclient.screen.TrsTitleScreen
            || (screen instanceof dev.theredstonee.trsclient.screen.TrsUiScreen ui
                && ui.ui() instanceof dev.theredstonee.trsclient.core.ui.menu.ModMenu);
        return custom?TFL_MENU_FONT:original;
    }
}
*///?} else {
public abstract class ClientMenuFontMixin { }
//?}
