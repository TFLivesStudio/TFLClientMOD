package dev.theredstonee.trsclient.ornithe.mixin;

import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.menu.options.VideoOptionsScreen;
import net.minecraft.client.options.GameOptions;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(VideoOptionsScreen.class)
public interface VideoOptionsAccess {
  @Accessor("VIDEO_OPTIONS")
  static GameOptions.Option[] tfl$options() {
    throw new AssertionError();
  }

  @Accessor("parent")
  Screen tfl$parent();
}
