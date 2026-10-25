package dev.theredstonee.trsclient.ornithe.mixin;
import net.minecraft.client.gui.ToastGui;
import net.minecraft.stat.achievement.AchievementStat;
import net.minecraft.stat.achievement.Achievements;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
/** Suppress only the repeated basic inventory tutorial, never normal achievement/important messages. */
@Mixin(ToastGui.class)
public abstract class InventoryTutorialMixin {
    @Inject(method="setTutorial",at=@At("HEAD"),cancellable=true)
    private void tfl$inventoryTutorial(AchievementStat achievement,CallbackInfo ci){
        if(!dev.theredstonee.trsclient.core.product.Product.safeMode()&&achievement==Achievements.OPEN_INVENTORY)ci.cancel();
    }
}
