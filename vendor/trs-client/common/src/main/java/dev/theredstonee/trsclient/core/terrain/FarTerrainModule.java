package dev.theredstonee.trsclient.core.terrain;

import dev.theredstonee.trsclient.core.module.*;

/** Shared product controls; adapters provide the appropriate real terrain renderer. */
public final class FarTerrainModule extends dev.theredstonee.trsclient.core.module.Module {
    public final NumberSetting distance=add(new NumberSetting("distance","LOD distance in chunks",96,32,256,16,""));
    public final NumberSetting gpuBudget=add(new NumberSetting("gpuBudget","Geometry memory budget (MiB)",128,32,512,32,""));
    public final NumberSetting diskBudget=add(new NumberSetting("diskBudget","Persistent cache budget (MiB)",512,128,4096,128,""));
    public FarTerrainModule(){super("farTerrain","Far Terrain","Real distant terrain from received and remembered chunks",true);category(Category.WORLD);icon("layers");}
}
