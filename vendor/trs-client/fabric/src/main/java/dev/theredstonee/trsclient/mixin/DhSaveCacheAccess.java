//? if >=26.3 {
/*package dev.theredstonee.trsclient.mixin;

import com.seibel.distanthorizons.core.file.structure.ClientOnlySaveStructure;
import com.seibel.distanthorizons.core.wrapperInterfaces.world.ILevelWrapper;
import java.io.File;
import java.util.concurrent.ConcurrentHashMap;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(value=ClientOnlySaveStructure.class,remap=false)
public interface DhSaveCacheAccess {
    @Accessor("levelWrapperToFileMap") ConcurrentHashMap<ILevelWrapper,File> tfl$levelPaths();
}
*///?}
