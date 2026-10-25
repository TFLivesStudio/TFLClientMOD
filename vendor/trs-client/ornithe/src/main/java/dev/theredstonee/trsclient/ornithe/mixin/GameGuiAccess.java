package dev.theredstonee.trsclient.ornithe.mixin;

import net.minecraft.client.gui.GameGui;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(GameGui.class)
public interface GameGuiAccess {
  @Accessor("title")
  String tfl$title();

  @Accessor("subtitle")
  String tfl$subtitle();

  @Accessor("titleTime")
  int tfl$titleTime();

  @Accessor("titleFadeInTime")
  int tfl$fadeIn();

  @Accessor("titleDuration")
  int tfl$duration();

  @Accessor("titleFadeOutTime")
  int tfl$fadeOut();
}
